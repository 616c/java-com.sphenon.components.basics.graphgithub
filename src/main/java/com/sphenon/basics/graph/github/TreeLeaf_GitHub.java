package com.sphenon.basics.graph.github;

/****************************************************************************
  Copyright 2001-2024 Sphenon GmbH

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations
  under the License.
*****************************************************************************/

import com.sphenon.basics.context.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.encoding.*;
import com.sphenon.basics.validation.returncodes.*;
import com.sphenon.basics.locating.*;
import com.sphenon.basics.locating.factories.*;
import com.sphenon.basics.system.*;
import com.sphenon.basics.expression.*;
import com.sphenon.basics.data.*;

import com.sphenon.basics.graph.*;
import com.sphenon.basics.graph.tplinst.*;
import com.sphenon.basics.graph.classes.*;
import com.sphenon.basics.graph.javaresources.factories.*;

import java.io.*;

import java.util.Vector;
import java.util.HashMap;

public class TreeLeaf_GitHub extends TreeNode_GitHub implements TreeLeaf {

    protected TreeLeaf_GitHub (CallContext context, TreeNode_GitHub parent, String id) {
        super(context, parent, id);
    }

    static public TreeLeaf_GitHub create (CallContext context, TreeNode_GitHub parent, String id) {
        return new TreeLeaf_GitHub(context, parent, id);
    }

    public Vector_TreeNode_long_ getChilds(CallContext context) {
        return null;
    }

    public TreeNode tryGetOrCreateChild(CallContext context, String id, NodeType node_type) {
        return null;
    }

    public NodeContent getContent(CallContext context) {
        String path = this.getPath(context);
        final String url = "https://raw.githubusercontent.com/" + this.getUser(context) + "/" + this.getRepository(context) + "/" + this.getReference(context) + (path == null || path.isEmpty() ? "" : ("/" + path));
        return new NodeContent_Data() {
            public Data getData(CallContext context) {
                return Data_MediaObject_URL.create(context, url, null, null);
            }
        };
    }
}
