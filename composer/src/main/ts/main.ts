import { getConfig, log } from "./imports";
import { Module } from "./types";

import operaDownloadButton from "./modules/operaDownloadButton";
import firstCreatedUsername from "./modules/firstCreatedUsername";
import bypassCameraRollSelectionLimit from "./modules/bypassCameraRollSelectionLimit";
import composerTheming from "./modules/composerTheming";


try {
    const config = getConfig();

    if (config.composerLogs) {
        ["log", "error", "warn", "info", "debug"].forEach(method => {
            console[method] = (...args: any) => log(method, Array.from(args).join(" "));
        })
    }

    const modules: Module[] = [
        operaDownloadButton, 
        firstCreatedUsername,
        bypassCameraRollSelectionLimit,
        composerTheming
    ];

    modules.forEach(module => {
        if (!module.enabled(config)) return
        try {
            module.init();
        } catch (e) {
            console.error(`failed to initialize module ${module.name}`, e, e.stack);
        }
    });

    console.log("composer modules loaded!");
} catch (e) {
    log("error", "Failed to load composer modules\n" + e + "\n" + e.stack)
}
