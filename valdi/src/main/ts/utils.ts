export function dumpObject(obj: any, indent = 0) {
    if (typeof obj !== "object") return console.log(obj);
    let prefix = ""
    for (let i = 0; i < indent; i++) {
        prefix += "    ";
    }
    for (let key of Object.keys(obj)) {
        try {
            console.log(prefix, key, typeof obj[key], obj[key]);
            if (key == "renderer") continue
            if (typeof obj[key] === "object" && indent < 10) dumpObject(obj[key], indent + 1);
        } catch (e) {}
    }
}

export function proxyProperty(module: any, functionName: string, handler: any) {
    if (!module || !module[functionName]) {
        console.warn("Function not found", functionName);
        return;
    }
    module[functionName] = new Proxy(module[functionName], {
        apply: (a, b, c) => handler(a, b, c),
        construct: (a, b, c) => handler(a, b, c)
    });
}

export function interceptComponent(moduleName: string, className: string, functions: any) {
    proxyProperty(require(moduleName), className, (target: any, args: any[], newTarget: any) => {
        let initProxy = functions["<init>"]
        let component: any;

        if (initProxy) {
            initProxy(args, (newArgs: any[]) => {
                component = Reflect.construct(target, newArgs || args, newTarget);
            });
        } else {
            component = Reflect.construct(target, args, newTarget);
        }

        for (let funcName of Object.keys(functions)) {
            if (funcName == "<init>" || !component[funcName]) continue
            proxyProperty(component, funcName, (target: any, thisArg: any, argumentsList: any[]) => {
                let result: any;
                try {
                    functions[funcName](component, argumentsList, (newArgs: any[]) => {
                        result = Reflect.apply(target, thisArg, newArgs || argumentsList);
                    });
                } catch (e) {
                    console.error("Error in", funcName, e);
                }
                return result;
            });
        }

        return component;
    })
}
