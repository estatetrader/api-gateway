class GenericType {
    static DATE_FORMAT = new Intl.DateTimeFormat('zh-CN', {
        hour12: false,
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second:'2-digit'
    });

    // typeName: string;
    // typeArgs: GenericType[];
    // typeVar: string;
    // possibleTypes: {string: GenericType};

    constructor({typeName, typeArgs, typeVar, possibleTypes}) {
        this.typeName = typeName;
        this.typeArgs = typeArgs ? typeArgs.map(t => GenericType.of(t)) : null;
        this.typeVar = typeVar;
        if (possibleTypes) {
            this.possibleTypes = Object.fromEntries(Object.entries(possibleTypes).map(entry => [entry[0], GenericType.of(entry[1])]))
        }
    }

    static of(genericType) {
        return genericType instanceof GenericType ? genericType : new GenericType(genericType);
    }

    static normalType(typeName) {
        return new GenericType({typeName: typeName});
    }

    static parameterizedType(typeName, typeArgs) {
        return new GenericType({typeName: typeName, typeArgs: typeArgs});
    }

    static listType(elementType) {
        return new GenericType({typeName: "list", typeArgs: [elementType]});
    }

    static typeVariable(typeVar) {
        return new GenericType({typeVar: typeVar});
    }

    static unionType(possibleTypes) {
        return new GenericType({possibleTypes: possibleTypes});
    }

    isBasicType() {
        return this.typeName
            && ["boolean", "byte", "char", "short", "int", "long", "float", "double", "string", "date"]
                .includes(this.typeName)
    }

    resolveTypeVars(binding) {
        if (this.typeVar) {
            let typeArg = binding.get(this.typeVar);
            return typeArg ? typeArg : this;
        } else if (this.typeName) {
            if (!this.typeArgs) {
                return this;
            }
            let newTypeArgs = [];
            for (let typeArg of this.typeArgs) {
                newTypeArgs.push(typeArg.resolveTypeVars(binding));
            }
            return GenericType.parameterizedType(this.typeName, newTypeArgs);
        } else if (this.possibleTypes) {
            let newPossibleTypes = {};
            for (let [key, possibleType] of Object.entries(this.possibleTypes)) {
                newPossibleTypes[key] = possibleType.resolveTypeVars(binding);
            }
            return GenericType.unionType(newPossibleTypes);
        } else {
            return this;
        }
    }

    get title() {
        if (this._title) {
            return this._title;
        }

        if (this.typeName === "list") {
            this._title = '[' + this.typeArgs[0].title + ']';
        } else if (this.typeName === 'map') {
            this._title = '[' + this.typeArgs[0].title + ': ' + this.typeArgs[1].title + ']';
        } else if (this.typeArgs) {
            this._title = this.typeName + "<" + this.typeArgs.map(x => x.title).join(", ") + ">";
        } else if (this.typeName) {
            this._title = this.typeName;
        } else if (this.typeVar) {
            this._title = this.typeVar;
        } else if (this.possibleTypes) {
            let possibleTypes = Object.entries(this.possibleTypes).map(entry => entry[0]);
            if (possibleTypes.length > 3) {
                this._title = '(' + possibleTypes.slice(0, 3).join(" | ") + " | .." + ')';
            } else {
                this._title = '(' + possibleTypes.join(" | ") + ')';
            }
        } else {
            this._title = "null";
        }

        return this._title;
    }

    toString() {
        if (this._string) {
            return this._string;
        }

        if (this.typeName === "list") {
            this._string = '[' + this.typeArgs[0] + ']';
        } else if (this.typeName === 'map') {
            this._string = '[' + this.typeArgs[0] + ': ' + this.typeArgs[1] + ']';
        } else if (this.typeArgs) {
            this._string = this.typeName + "<" + this.typeArgs.join(", ") + ">";
        } else if (this.typeName) {
            this._string = this.typeName;
        } else if (this.typeVar) {
            this._string = this.typeVar;
        } else if (this.possibleTypes) {
            this._string = Object.entries(this.possibleTypes).map(entry => entry[0] + ":" + entry[1]).join(" | ");
        } else {
            this._string = "null";
        }

        return this._string;
    }

    toHtml(api) {
        if (this.typeName === "list") {
            return '<span class="type-name-left-square">[</span>'
                + this.typeArgs[0].toHtml(api)
                + '<span class="type-name-right-square">]</span>';
        } else if (this.typeName === 'map') {
            return '<span class="type-name-left-square">[</span>'
                + this.typeArgs[0].toHtml(api)
                + '<span class="type-map-colon">:</span>'
                + this.typeArgs[1].toHtml(api)
                + '<span class="type-name-right-square">]</span>';
        } else if (this.isBasicType()) {
            return '<span class="type-name">' + this.typeName + '</span>';
        } else if (this.typeName) {
            let href = '#' + this.targetHashId(api);
            let prefix = '<a href="' + href + '" class="type-name">' + this.typeName + '</a>';
            let postfix;
            if (this.typeArgs) {
                let args = this.typeArgs.map(arg => arg.toHtml(api)).join('<span class="type-comma">, </span>');
                postfix = '<span class="type-left-brace">&lt;</span>'
                    + args +
                    '<span class="type-right-brace">&gt;</span>';
                return '<span class="type-generic">' + prefix + postfix + '</span>';
            } else {
                return prefix;
            }
        } else if (this.typeVar) {
            return '<span class="type-var">' + this.typeVar + '</span>';
        } else if (this.possibleTypes) {
            let pts = Object.entries(this.possibleTypes)
                .map(entry => entry[1].toHtmlForPossibleType(entry[0], api))
                .join('<span class="type-pipe"> | </span>');
            return '' +
                '<span class="type-union">' +
                '<span class="type-left-brace">(</span>' +
                pts +
                '<span class="type-right-brace">)</span>' +
                '</span>';
        } else {
            return '<span>null</span>';
        }
    }

    toHtmlForPossibleType(typeKey, api) {
        let href = '#' + this.targetHashId(api);
        return '<a href="' + href + '" class="possible-type-key" title="' + this.toString() + '">' + typeKey + '</a>';
    }

    targetHashId(api) {
        return "api_" + api.methodName + "_type_" + this.toString().replace(/[^a-zA-Z0-9_]/, '');
    }

    generateExampleValue(ctx) {
        if (this.typeName === "list") {
            let eleType = this.typeArgs[0];
            if (eleType.possibleTypes) {
                return Object.entries(eleType.possibleTypes).map(entry => {
                    let value = entry[1].generateExampleValue(ctx);
                    value['@classKey'] = entry[0];
                    return value;
                });
            } else {
                return [
                    eleType.generateExampleValue(ctx)
                ]
            }
        } else if (this.typeName === "map") {
            let keyType = this.typeArgs[0];
            let valueType = this.typeArgs[1];
            if (valueType.possibleTypes) {
                let map = {};
                for (let entry of Object.entries(valueType.possibleTypes)) {
                    let key = keyType.generateExampleValue(ctx);
                    let value = entry[1].generateExampleValue(ctx);
                    value['@classKey'] = entry[0];
                    map[key.toString()] = value;
                }
                return map;
            } else {
                let map = {};
                let key = keyType.generateExampleValue(ctx);
                map[key.toString()] = valueType.generateExampleValue(ctx);
                return map;
            }
        } else if (this.typeName === 'string') {
            return 'string' + ctx.nextSeq();
        } else if (this.typeName === 'char') {
            return String.fromCharCode(ctx.nextSeq() % 26 + 'a'.charCodeAt(0));
        } else if (this.typeName === 'date') {
            return GenericType.DATE_FORMAT.format(new Date()).replace('/', '-');
        } else if (this.typeName === 'boolean') {
            return ctx.nextSeq() % 2 === 0;
        } else if (this.typeName === 'byte') {
            return ctx.nextSeq() % 256;
        } else if (this.typeName === 'short' || this.typeName === 'int' || this.typeName === 'long') {
            return ctx.nextSeq();
        } else if (this.typeName === 'float' || this.typeName === 'double') {
            return Number.parseFloat(ctx.nextSeq() + '.' + ctx.nextSeq());
        } else if (this.typeName) {
            let struct = ctx.api.referredStructs[this.toString()];
            return struct ? struct.generateExampleValue(ctx) : {};
        } else if (this.typeVar) {
            return null;
        } else if (this.possibleTypes) {
            let pts = Object.entries(this.possibleTypes);
            let pt = pts[ctx.nextSeq() % pts.length];
            let value = pt[1].generateExampleValue(ctx);
            value['@classKey'] = pt[0];
            return value;
        } else {
            return null;
        }
    }
}

class ExampleValues {
    static parse(type, text) {
        if (typeof text !== 'string' || type.isBasicType()) {
            return text;
        } else if (text) {
            try {
                return JSON.parse(text);
            } catch (e) {
                console.debug('failed to parse the example value ' + text + ' for type ' + type, e);
            }
        }
    }

    static format(exampleValue) {
        if (typeof exampleValue === 'object') {
            return JSON.stringify(exampleValue, null, 2);
        } else {
            return exampleValue;
        }
    }
}

class RandomContext {
    constructor(api) {
        this.seq = 1;
        this.structPath = [];
        this.api = api;
    }

    pushStruct(struct) {
        if (this.structPath.includes(struct)) {
            return false;
        } else {
            this.structPath.push(struct);
            return true;
        }
    }

    popStruct() {
        this.structPath.pop();
    }

    nextSeq() {
        return this.seq++;
    }
}

class FieldInfo {
    // name: string;
    // type: GenericType;
    // desc: string;
    // deprecated: boolean;
    // exampleValue: string;

    constructor({name, type, desc, deprecated, exampleValue}) {
        this.name = name;
        this.type = GenericType.of(type);
        this.desc = desc;
        this.deprecated = deprecated;
        this.exampleValue = ExampleValues.parse(this.type, exampleValue);
    }

    asBoundField(variableBinding) {
        return new FieldInfo({
            name: this.name,
            type: this.type.resolveTypeVars(variableBinding),
            desc: this.desc,
            deprecated: this.deprecated,
            exampleValue: this.exampleValue
        });
    }

    generateExampleValue(ctx) {
        return this.exampleValue || this.type.generateExampleValue(ctx);
    }

    formattedExampleValue(api) {
        return ExampleValues.format(this.generateExampleValue(new RandomContext(api)));
    }
}

class TypeStruct {
    // name: string;
    // groupName: string;
    // fields: FieldInfo[];
    // typeVars: string[];

    constructor({name, groupName, fields, typeVars}) {
        this.name = name;
        this.groupName = groupName;
        this.fields = fields.map(f => new FieldInfo(f));
        this.typeVars = typeVars;
    }

    get signature() {
        if (!this.typeVars || this.typeVars.length === 0) {
            return this.name;
        }
        let sb = [this.name];
        sb.push('<');
        for (let i = 0; i < this.typeVars.length; i++) {
            if (i > 0) {
                sb.push(", ");
            }
            sb.push(this.typeVars[i]);
        }
        sb.push('>');
        return sb.join("");
    }

    variableBinding(actualType) {
        if (this.typeVars == null || this.typeVars.length === 0) {
            return new Map();
        }
        if (!actualType.typeArgs || actualType.typeArgs.length !== this.typeVars.length) {
            throw new Error("the given actual type " + actualType
                + " does not matches the struct " + this.signature);
        }
        let binding = new Map();
        for (let i = 0; i < this.typeVars.length; i++) {
            binding.set(this.typeVars[i], actualType.typeArgs[i]);
        }
        return binding;
    }

    asBoundStruct(actualType) {
        if (this.name !== actualType.typeName) {
            throw new Error("struct name of " + this.signature + " does not match " + actualType.toString());
        }
        let binding = this.variableBinding(actualType);
        let newFields = this.fields.map(f => f.asBoundField(binding));
        return new BoundStruct(
            this.name,
            this.groupName,
            actualType,
            newFields
        );
    }
}

class BoundStruct {
    // name: string;
    // groupName: string;
    // type: GenericType;
    // fields: FieldInfo[];

    constructor(name, groupName, type, fields) {
        this.name = name;
        this.groupName = groupName;
        this.type = type;
        this.fields = fields;
    }

    generateExampleValue(ctx) {
        if (ctx.pushStruct(this)) {
            let result = Object.fromEntries(this.fields.map(f => [f.name, f.generateExampleValue(ctx)]));
            ctx.popStruct();
            return result;
        } else {
            return {};
        }
    }

    matchesKeyword(keyword) {
        return keyword.length > 0 && this.name.toUpperCase().includes(keyword.toUpperCase());
    }
}

class MethodInfo {
    // methodName: string;
    // groupName: string;
    // returnType: GenericType;
    // description: string;
    // detail: string;
    // securityLevel: string;
    // parameters: ParameterInfo[];
    // errorCodes: [];
    // groupOwner: string;
    // methodOwner: string;
    // jarFile: string;
    // exampleValue: string;
    // referredStructs: BoundStruct[];

    constructor({methodName, groupName, returnType, description, detail, securityLevel, parameters,
                    errorCodes, groupOwner, methodOwner, jarFile, exampleValue}, allStructs) {
        this.methodName = methodName;
        this.groupName = groupName;
        this.returnType = GenericType.of(returnType);
        this.description = description;
        this.detail = detail;
        this.securityLevel = securityLevel;
        this.parameters = parameters.map(p => new ParameterInfo(p));
        this.errorCodes = errorCodes?.map(c => new CodeInfo(c));
        this.groupOwner = groupOwner;
        this.methodOwner = methodOwner;
        this.jarFile = jarFile;
        this.exampleValue = ExampleValues.parse(this.returnType, exampleValue);

        let referredStructs = {};
        let issues = [];
        for (let param of this.parameters) {
            structClosure(param.type, allStructs, referredStructs, issues);
        }
        structClosure(this.returnType, allStructs, referredStructs, issues);
        this.referredStructs = Object.values(referredStructs);
        this.issues = issues;

        let searchableLines = [];
        searchableLines.push(methodName.toUpperCase());
        if (description) {
            searchableLines.push(this.description.toUpperCase())
        }
        for (let struct of this.referredStructs) {
            searchableLines.push(struct.name.toUpperCase());
        }
        for (let issue of issues) {
            searchableLines.push(issue.toUpperCase());
        }
        this.searchableText = searchableLines.join('\n');
    }

    get targetHashId() {
        return "api_" + this.methodName;
    }

    generateExampleValue(ctx) {
        return this.exampleValue || this.returnType.generateExampleValue(ctx);
    }

    formattedExampleValue() {
        return ExampleValues.format(this.generateExampleValue(new RandomContext(this)));
    }

    matchesKeyword(keyword) {
        return this.searchableText.includes(keyword);
    }
}

class ParameterInfo {
    // name: string;
    // type: GenericType;
    // description: string;
    // required: boolean;
    // encryptionMethod: string;
    // exampleValue: string;

    constructor({name, type, description, required, encryptionMethod, exampleValue}) {
        this.name = name;
        this.type = GenericType.of(type);
        this.description = description;
        this.required = required;
        this.encryptionMethod = encryptionMethod;
        this.exampleValue = ExampleValues.parse(this.type, exampleValue);
    }

    generateExampleValue(ctx) {
        return this.exampleValue || this.type.generateExampleValue(ctx);
    }

    formattedExampleValue(api) {
        return ExampleValues.format(this.generateExampleValue(new RandomContext(api)));
    }
}

class CodeInfo {
    // code: number;
    // name: string;
    // desc: string;
    // service: string;
    // isDesign: boolean;

    constructor({code, name, desc, service, exposedToClient}) {
        this.code = code;
        this.name = name;
        this.desc = desc;
        this.service = service;
        this.exposedToClient = exposedToClient;
    }
}

class ApiDocument {
    // apis: MethodInfo[];
    // codes: CodeInfo[];
    // structures: {string: TypeStruct};
    // commonParams: {}[];

    constructor({apis, codes, structures, commonParams}) {
        let structMap = Object.fromEntries(structures.map(s => [s.name, new TypeStruct(s)]));
        this.apis = apis.map(m => new MethodInfo(m, structMap));
        this.codes = codes.map(c => new CodeInfo(c));
        this.structures = structMap;
        this.commonParams = commonParams;
    }
}

function structClosure(genericType, allStructs, closure, issues) {
    if (genericType.typeName === "list") {
        structClosure(genericType.typeArgs[0], allStructs, closure, issues);
    } else if (genericType.typeName === 'map') {
        structClosure(genericType.typeArgs[1], allStructs, closure, issues);
    } else if (genericType.isBasicType()) {
        // nothing to do
    } else if (genericType.typeName) {
        if (closure[genericType.toString()]) {
            return;
        }
        let struct = allStructs[genericType.typeName];
        if (!struct) {
            issues.push('could not find struct ' + genericType.typeName);
            return;
        }
        let boundStruct = struct.asBoundStruct(genericType);
        closure[genericType.toString()] = boundStruct;
        if (genericType.typeArgs) {
            for (let typeArg of genericType.typeArgs) {
                structClosure(typeArg, allStructs, closure, issues);
            }
        }
        for (let field of boundStruct.fields) {
            structClosure(field.type, allStructs, closure, issues);
        }
    } else if (genericType.possibleTypes) {
        for (let possibleType of Object.values(genericType.possibleTypes)) {
            structClosure(possibleType, allStructs, closure, issues);
        }
    }
}

let app = new Vue({
    el: '#app',
    data: {
        scopeValue: 'all',
        queryValue: '',
        expandedGroupNames: [],
        selectedApi: null,
        searchResult: [],
        loaded: false,
        apiSchemaError: null,
        groups: [],
        apis: [],
        apiSchema: ApiDocument
    },
    computed: {
        query: {
            get: function () {
                return this.queryValue;
            },
            set: function (value) {
                this.queryValue = value;
                this.updateSearchResult();
            }
        },
        selectedGroupName: function () {
            let selectedApi = this.selectedApi;
            if (selectedApi) {
                return selectedApi.methodName.substring(0, selectedApi.methodName.indexOf('.'));
            }
        },
        scope: {
            get: function () {
                return this.scopeValue;
            },
            set: function (value) {
                this.scopeValue = value;
                this.updateFilterResult();
                this.updateSearchResult();
            }
        }
    },
    methods: {
        expandGroup: function (group) {
            let index = this.expandedGroupNames.indexOf(group.groupName);
            if (index < 0) {
                this.expandedGroupNames.push(group.groupName);
            } else {
                if (this.selectedApi && this.selectedApi.methodName.startsWith(group.groupName + '.')) {
                    this.selectedApi = null;
                }
                this.expandedGroupNames.splice(index, 1);
            }
        },
        selectApi: function (api) {
            this.selectedApi = api;
            $(document).scrollTop(0);
            location.hash = '';
        },
        updateSearchResult: function () {
            let query = this.query.trim().toUpperCase();
            if (query.length === 0) {
                this.searchResult = this.groups;
                this.expandedGroupNames = [];
                return;
            }
            let result = [];

            let groupNamesNeedToExpand = [];

            for (let group of this.groups) {
                let searchedApis = [];
                for (let api of group.apis) {
                    if (api.matchesKeyword(query)) {
                        searchedApis.push(api);
                    }
                }

                if (searchedApis.length > 0) {
                    result.push({
                        groupName: group.groupName,
                        apis: searchedApis
                    });
                    groupNamesNeedToExpand.push(group.groupName);
                }
            }

            this.searchResult = result;
            this.expandedGroupNames = groupNamesNeedToExpand;

            if (result.length === 1 && result[0].apis.length === 1) {
                let groupName = result[0].groupName;
                if (!this.expandedGroupNames.includes(groupName)) {
                    this.expandedGroupNames.push(groupName);
                }
            }
        },
        updateApiSchema: function (apiSchema) {
            if (apiSchema.error) {
                this.apiSchemaError = apiSchema.error;
                this.loaded = true;
                return;
            }

            this.apiSchema = new ApiDocument(apiSchema);
            this.loaded = true;
            this.updateFilterResult();
            this.updateSearchResult();
        },
        updateFilterResult: function () {
            let groups = [];
            let apis = [];
            for (let api of this.apiSchema.apis) {
                let scope = this.scope;
                if (scope === 'client' && !["Anonym", "User", "UnidentifiedUser", "RegisteredDevice"].includes(api.securityLevel)) {
                    continue;
                }
                if (scope === 'backend' && !["AuthorizedUser", "InternalUser", "SubSystem"].includes(api.securityLevel)) {
                    continue;
                }
                if (scope === 'internal' && !["Internal"].includes(api.securityLevel)) {
                    continue;
                }
                if (scope === 'integrated' && !["Integrated"].includes(api.securityLevel)) {
                    continue;
                }

                apis.push(api);

                let groupName = api.methodName.substring(0, api.methodName.indexOf('.'));
                let group = groups.find(g => g.groupName === groupName);
                if (!group) {
                    groups.push(group = {groupName: groupName, apis: []});
                }
                group.apis.push(api);
            }
            this.apis = apis;
            this.groups = groups;
        }
    }
});