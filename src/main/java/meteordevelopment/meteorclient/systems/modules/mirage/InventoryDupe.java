/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.mirage;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class InventoryDupe extends Module {
    public InventoryDupe() {
        super(Categories.MirageClient, "InvDupe [M]", "InventoryDupe only works on servers with the version 1.17. (Not any version after or before.)");
    }
}
