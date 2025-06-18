use std::ffi::{c_void, CStr};

use jni::{objects::{JByteArray, JMethodID, JValue}, signature::ReturnType};
use nix::libc;
use once_cell::sync::OnceCell;

use crate::{common::{self}, def_hook, dobby_hook, sig};

#[repr(C)]
#[derive(Copy, Clone)]
struct RefCountedSliceByteBuffer {
    ref_counter: *mut c_void,
    length: usize,
    data: *mut u8
}

#[repr(C)]
struct GrpcByteBuffer {
    reserved: *mut c_void,
    type_: *mut c_void,
    compression: *mut c_void,
    slice_buffer: *mut RefCountedSliceByteBuffer
}

static NATIVE_LIB_ON_UNARY_CALL_METHOD: OnceCell<JMethodID> = OnceCell::new();

def_hook!(
    unary_call,
    *mut c_void,
    |unk1: *mut c_void, uri: *const u8, grpc_byte_buffer: *mut *mut GrpcByteBuffer, unk4: *mut c_void, unk5: *mut c_void, unk6: *mut c_void| {
        macro_rules! call_original {
            () => {
                unary_call_original.unwrap()(unk1, uri, grpc_byte_buffer, unk4, unk5, unk6)
            };
        }

        // make a local copy of the slice buffer
        let mut slice_buffer = *(**grpc_byte_buffer).slice_buffer;

        if slice_buffer.ref_counter.is_null() {
            return call_original!();
        }

        let java_vm = common::java_vm();
        let mut env = java_vm.get_env().expect("Failed to get JNIEnv");

        let slice_buffer_length = slice_buffer.length as usize;
        let jni_buffer = env.new_byte_array(slice_buffer_length as i32).expect("Failed to create new byte array");
        env.set_byte_array_region(&jni_buffer, 0, std::slice::from_raw_parts(slice_buffer.data as *const i8, slice_buffer_length)).expect("Failed to set byte array region");

        let uri_str = CStr::from_ptr(uri).to_str().unwrap();

        let native_request_data_object = env.call_method_unchecked(
            common::native_lib_instance(),
            NATIVE_LIB_ON_UNARY_CALL_METHOD.get().unwrap(),
            ReturnType::Object,
            &[
                JValue::from(&env.new_string(uri_str).unwrap()).as_jni(),
                JValue::from(&jni_buffer).as_jni()
            ]
        ).expect("Failed to call onNativeUnaryCall method").l().unwrap();

        if native_request_data_object.is_null() {
            return call_original!();
        }

        let is_canceled = env.get_field(&native_request_data_object, "canceled", "Z").expect("Failed to get canceled field").z().unwrap();

        if is_canceled {
            info!("canceled request for {}", uri_str);
            return std::ptr::null_mut();
        }

        let new_buffer: JByteArray = env.get_field(&native_request_data_object, "buffer", "[B").expect("Failed to get buffer field").l().unwrap().into();
        let new_buffer_length = env.get_array_length(&new_buffer).expect("Failed to get array length") as usize;

        let mut new_buffer_data = Box::new(vec![0i8; new_buffer_length]);
        env.get_byte_array_region(&new_buffer, 0, new_buffer_data.as_mut_slice()).expect("Failed to get byte array region");

        let ref_counter_struct_size = (slice_buffer.data as usize) - (slice_buffer.ref_counter as usize);

        //we need to allocate a new ref_counter struct and copy the old ref_counter and the new_buffer to it
        let new_ref = {
            let new_ref = libc::malloc(ref_counter_struct_size + new_buffer_length) as *mut c_void;
            libc::memcpy(new_ref, slice_buffer.ref_counter, ref_counter_struct_size);
            libc::memcpy(new_ref.offset(ref_counter_struct_size as isize), new_buffer_data.as_ptr() as *const c_void, new_buffer_length);
            libc::free(slice_buffer.ref_counter);
            new_ref
        };

        slice_buffer.ref_counter = new_ref;
        slice_buffer.length = new_buffer_length;
        slice_buffer.data = new_ref.offset(ref_counter_struct_size as isize) as *mut u8;

        // update the grpc byte buffer
        *(**grpc_byte_buffer).slice_buffer = slice_buffer;

        debug!("unary_call {}", uri_str);

        call_original!()
    }
);

pub fn init() {
    if let Some(signature) = sig::find_signature(
        &common::CLIENT_MODULE, 
        "A8 03 1F F8 ?? 00 00 94 ?? ?? ?? 91", -0x48,
        "0A 90 00 F0 3F F9", -0x37
    ) {
        dobby_hook!(signature as *mut c_void, unary_call);
        common::attach_jni_env(|env| {
            NATIVE_LIB_ON_UNARY_CALL_METHOD.set(
                env.get_method_id(
                    env.get_object_class(common::native_lib_instance()).unwrap(),
                        "onNativeUnaryCall", 
                        "(Ljava/lang/String;[B)Lme/rhunk/snapenhance/nativelib/NativeRequestData;"
                ).expect("Failed to get onNativeUnaryCall method id")
            ).expect("unary call method already set");
        });
    } else {
        error!("Can't find unaryCall signature");
    }
}