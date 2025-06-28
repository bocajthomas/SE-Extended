package me.rhunk.snapenhance.common.config

/*
  Due to recent resource obfuscation, some UI features will no longer work because it depends on non obfuscated resources
*/
val RES_OBF_VERSION_CHECK = VersionCheck(maxVersion = ("13.7.0.42" to 157172))

/*
  After this version, Snapchat will start detecting modifications to their app (to be confirmed)
*/
val MOD_DETECTION_VERSION_CHECK = VersionCheck(maxVersion = ("12.33.1.19 (84704)" to 84704))