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

import com.sphenon.basics.graph.*;
import com.sphenon.basics.graph.tplinst.*;
import com.sphenon.basics.graph.classes.*;
import com.sphenon.basics.graph.javaresources.factories.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.*;

import java.util.Vector;
import java.util.HashMap;

public class TreeNode_GitHub extends TreeNode_BaseImpl {

    static final public Class _class = TreeNode_GitHub.class;

    static protected long notification_level;
    static public    long adjustNotificationLevel(long new_level) { long old_level = notification_level; notification_level = new_level; return old_level; }
    static public    long getNotificationLevel() { return notification_level; }
    static { notification_level = NotificationLocationContext.getLevel(_class); };

    protected String                user;
    protected String                repository;
    protected String                reference;
    protected String                id;
    protected TreeNode_GitHub       parent;
    protected Location              location;

    protected TreeNode_GitHub (CallContext context, String user, String repository, String reference) {
        super(context);
        this.user       = user;
        this.repository = repository;
        this.reference  = reference;
        this.id         = null;
        this.parent     = null;

        if ((notification_level & Notifier.DIAGNOSTICS) != 0) { NotificationContext.sendSelfDiagnostics(context, "New TreeNode_GitHub: '%(path)'", "path", this.getPath(context)); }
    }

    protected TreeNode_GitHub (CallContext context, TreeNode_GitHub parent, String id) {
        super(context);
        this.user       = parent.getUser(context);
        this.repository = parent.getRepository(context);
        this.reference  = parent.getReference(context);
        this.id         = id;
        this.parent     = parent;

        if ((notification_level & Notifier.DIAGNOSTICS) != 0) { NotificationContext.sendSelfDiagnostics(context, "New TreeNode_GitHub: '%(path)'", "path", this.getPath(context)); }
    }

    static public TreeNode_GitHub create (CallContext context, String user, String repository, String reference) {
        return new TreeNode_GitHub(context, user, repository, reference);
    }

    static public TreeNode_GitHub create (CallContext context, TreeNode_GitHub parent, String id) {
        return new TreeNode_GitHub(context, parent, id);
    }

    public String getUser(CallContext context) {
        return this.user;
    }

    public String getRepository(CallContext context) {
        return this.repository;
    }

    public String getReference(CallContext context) {
        return this.reference;
    }

    public String getId(CallContext context) {
        return (this.id != null ? this.id : (this.repository + "[" + this.reference + "]"));
    }

    public Location getLocation(CallContext context) {
        if (this.location == null) {
            String locator = "oorl://GitHub/" + this.getUser(context) + "/" + this.getRepository(context) + "/" + this.getReference(context) + "/" + this.getPath(context);
            try {
                this.location = Factory_Location.construct(context, Factory_Locator.construct(context, locator));
            } catch (ValidationFailure vf) {
                CustomaryContext.create((Context)context).throwAssertionProvedFalse(context, vf, "Internally created java resource locator '%(locator)' is invalid", "locator", locator);
                throw (ExceptionAssertionProvedFalse) null; // compiler insists
            }
        }
        return this.location;
    }

    public String getPath(CallContext context) {
        return (this.parent == null ? "" : (this.parent.getPath(context) + "/")) + (this.id == null ? "" : this.id);
    }

    public String getPathEncoded(CallContext context) {
        return (this.parent == null ? "" : (this.parent.getPath(context) + "/")) + Encoding.recode(context, this.id, Encoding.UTF8, Encoding.URI);
    }

    public TreeNode tryGetChild(CallContext context, String id) {
        String[] parts = id.split("/", 2);
        id = parts[0];
        this.getChilds(context);
        TreeNode child_node = this.getChildMap(context).get(id);
        return (child_node == null ? null : (parts.length == 1 ? child_node : child_node.tryGetChild(context, parts[1])));
    }

    public TreeNode tryGetOrCreateChild(CallContext context, String id, NodeType node_type) {
        return this.tryGetChild(context, id);
    }

    public long getLastModification(CallContext context) {
        return 0L; // this.file.lastModified();
    }

    public TreeNode tryGetParent(CallContext context) {
        return this.parent;
    }

    protected HashMap<String,TreeNode> child_map;
    protected Vector_TreeNode_long_ childs;

    protected HashMap<String,TreeNode> getChildMap(CallContext context) {
        if (this.child_map == null) {
            this.child_map = new HashMap<String,TreeNode>();
        }
        return this.child_map;
    }

    public Vector_TreeNode_long_ getChilds(CallContext context) {
        if (this.childs == null) {
            this.childs = Factory_Vector_TreeNode_long_.construct(context);
            this.filter_results = null;

            RESTRequest rr = new RESTRequest(context, 0 /* verbose */);
            String path = this.getPath(context);
            String url = "https://api.github.com/repos/" + this.getUser(context) + "/" + this.getRepository(context) + "/contents" + (path == null || path.isEmpty() ? "" : ("/" + path)) + "?ref=" + this.getReference(context);
            RESTRequest.Result result = rr.sendRequest(context, url, "GET", null, null, true, true, false, false);

            if (result.exception != null) {
                CustomaryContext.create((Context)context).throwEnvironmentError(context, result.exception, "Could not access github '%(url)'", "url", url);
                throw (ExceptionEnvironmentError) null; // compiler insists
            }

            if (result.node != null) {
                java.util.Iterator<JsonNode> elements = result.node.elements();
                while (elements.hasNext()) {
                    JsonNode child = elements.next();
                    String child_name = child.get("name").asText();
                    String child_type = child.get("type").asText();

                    NodeType nt = child_type != null && child_type.equals("file") ? NodeType.LEAF : NodeType.NODE;

                    TreeNode child_node = null;
                    if (nt == NodeType.LEAF) {
                        child_node = TreeLeaf_GitHub.create(context, this, child_name);
                    } else {
                        child_node = TreeNode_GitHub.create(context, this, child_name);
                    }
                    this.getChildMap(context).put(child_name, child_node);
                    this.childs.append(context, child_node);
                }
            }

            // [{"name":"README.md",
            //   "path":"README.md",
            //   "sha":"ffac040016133c0a43139d98de8f498158772537",
            //   "size":101,
            //   "url":"https://api.github.com/repos/616c/data-org.miotope.samples/contents/README.md?ref=master",
            //   "html_url":"https://github.com/616c/data-org.miotope.samples/blob/master/README.md",
            //   "git_url":"https://api.github.com/repos/616c/data-org.miotope.samples/git/blobs/ffac040016133c0a43139d98de8f498158772537",
            //   "download_url":"https://raw.githubusercontent.com/616c/data-org.miotope.samples/master/README.md",
            //   "type":"file",
            //   "_links":{"self":"https://api.github.com/repos/616c/data-org.miotope.samples/contents/README.md?ref=master",
            //             "git":"https://api.github.com/repos/616c/data-org.miotope.samples/git/blobs/ffac040016133c0a43139d98de8f498158772537",
            //             "html":"https://github.com/616c/data-org.miotope.samples/blob/master/README.md"}}]
        }
        return this.childs;
    }

    public boolean exists(CallContext context) {
        return true;
    }

    public String optionallyGetLinkTarget(CallContext context) {
        return null;
    }
}
