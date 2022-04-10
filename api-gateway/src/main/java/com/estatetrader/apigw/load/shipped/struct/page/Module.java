package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;
import java.util.List;

@Description("Module")
public class Module<S extends ModuleSetting, E extends ModuleEntity> implements Serializable {
    @Description("moduleId")
    public int moduleId;
    @Description("listId")
    public short listId;
    @Description("setting")
    public S setting;
    @Description("entities")
    public List<E> entities;

    public Module(int moduleId, short listId, S setting, List<E> entities) {
        this.moduleId = moduleId;
        this.listId = listId;
        this.setting = setting;
        this.entities = entities;
    }
}
