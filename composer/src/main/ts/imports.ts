import { Config, FriendInfo } from "./types";

declare var _getImportsFunctionName: string;
const remoteImports = require('composer_core/src/DeviceBridge')[_getImportsFunctionName]();

function callRemoteFunction(method: string, ...args: any[]): any | null {
    return remoteImports[method](...args);
}


export const log = (logLevel: string, message: string) => callRemoteFunction("log", logLevel, message);

export const getConfig = () => callRemoteFunction("getConfig") as Config;

export const downloadLastOperaMedia = (isLongPress: boolean) => callRemoteFunction("downloadLastOperaMedia", isLongPress);

export function getFriendInfoByUsername(username: string): FriendInfo | null {
    const friendInfo = callRemoteFunction("getFriendInfoByUsername", username);
    if (!friendInfo) return null;
    return JSON.parse(friendInfo);
}
