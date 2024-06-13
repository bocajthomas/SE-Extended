#pragma once

#include <pthread.h>
#include <dobby.h>


static pthread_mutex_t hook_mutex = PTHREAD_MUTEX_INITIALIZER;

static void inline SafeHook(void *addr, void *hook, void **original) {
    pthread_mutex_lock(&hook_mutex);
    DobbyHook(addr, hook, original);
    if (common::native_config->remap_executable) {
        mprotect((void *)((uintptr_t) *original & PAGE_MASK), PAGE_SIZE, PROT_EXEC);
    }
    pthread_mutex_unlock(&hook_mutex);
}