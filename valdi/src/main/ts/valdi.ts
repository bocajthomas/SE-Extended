import { runtimeName } from "./imports"

export const jsx = require(runtimeName + '_core/src/JSX').jsx;
export const assetCatalog = require(runtimeName + "_core/src/AssetCatalog")
export const style = require(runtimeName + "_core/src/Style");
export const colors = require("coreui/src/styles/semanticColors");
