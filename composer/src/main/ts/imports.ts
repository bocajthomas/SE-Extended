import { Config } from "./types";

declare var _getImportsFunctionName: string;
declare var _runtimeName: boolean;
export const runtimeName = _runtimeName;

const remoteImports = require(_runtimeName + '_core/src/DeviceBridge')[_getImportsFunctionName]();

function callRemoteFunction(method: string, ...args: any[]): any | null {
    return remoteImports[method](...args);
}


export const log = (logLevel: string, message: string) => callRemoteFunction("log", logLevel, message);

export const getConfig = () => callRemoteFunction("getConfig") as Config;

export const downloadLastOperaMedia = (isLongPress: boolean) => callRemoteFunction("downloadLastOperaMedia", isLongPress);

export function getFriendOriginalUsername(username: string): string | null {
    return callRemoteFunction("getFriendOriginalUsername", username);
}
