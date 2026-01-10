use std::{error::Error, sync::Mutex};
use jni::{objects::JObject, JNIEnv};
use crate::util::get_jni_string;

static NATIVE_CONFIG: Mutex<Option<NativeConfig>> = Mutex::new(None);

pub fn native_config() -> NativeConfig {
    NATIVE_CONFIG.lock().unwrap().as_ref().expect("NativeConfig not loaded").clone()
}

#[derive(Debug, Clone)]
pub(crate) struct NativeConfig {
    pub disable_bitmoji: bool,
    pub disable_metrics: bool,
    pub valdi_hooks: bool,
    pub custom_emoji_font_path: Option<String>,
}

impl NativeConfig {
    fn new(env: &mut JNIEnv, obj: JObject) -> Result<Self, Box<dyn Error>> {
        macro_rules! get_boolean {
            ($field:expr) => {
                env.get_field(&obj, $field, "Z")?.z()?
            };
        }

        macro_rules! get_string {
            ($field:expr) => {
                match env.get_field(&obj, $field, "Ljava/lang/String;")?.l()? {
                    jstring => if !jstring.is_null() {
                        Some(get_jni_string(env, jstring.into())?)
                    } else {
                        None
                    },
                }
            };
        }

        Ok(Self {
            disable_bitmoji: get_boolean!("disableBitmoji"),
            disable_metrics: get_boolean!("disableMetrics"),
            valdi_hooks: get_boolean!("valdiHooks"),
            custom_emoji_font_path: get_string!("customEmojiFontPath"),
        })
    }
}

pub fn load_config(mut env: JNIEnv, _class: JObject, obj: JObject)  {
    NATIVE_CONFIG.lock().unwrap().replace(
        NativeConfig::new(&mut env, obj).expect("Failed to load NativeConfig")
    );
    
    info!("Config loaded {:?}", native_config());
}