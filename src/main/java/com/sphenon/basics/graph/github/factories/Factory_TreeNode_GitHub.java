package com.sphenon.basics.graph.github.factories;

/****************************************************************************
  Copyright 2001-2018 Sphenon GmbH

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
import com.sphenon.basics.context.classes.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.factory.*;
import com.sphenon.basics.validation.returncodes.*;

import com.sphenon.basics.graph.*;
import com.sphenon.basics.graph.tplinst.*;
import com.sphenon.basics.graph.github.*;

import java.io.*;

public class Factory_TreeNode_GitHub implements Factory {

    /* -------------- extensible factory instantiation --------------------------------------------------------------------------------------- */
    static protected FactoryInstantiator<Factory_TreeNode_GitHub> factory_instantiator;
    static {
      CallContext context = RootContext.getInitialisationContext();
      factory_instantiator = new FactoryInstantiator(context, Factory_TreeNode_GitHub.class) { protected Factory_TreeNode_GitHub createDefault(CallContext context) { return new Factory_TreeNode_GitHub(context); } };
    };
    /* --------------------------------------------------------------------------------------------------------------------------------------- */
    static public Factory_TreeNode_GitHub newInstance (CallContext context) {
        return factory_instantiator.newInstance(context);
    }
    /* --------------------------------------------------------------------------------------------------------------------------------------- */

    protected Factory_TreeNode_GitHub (CallContext context) {
    }

    static public TreeNode construct (CallContext context, String user, String repository, String reference, String path) throws ValidationFailure {
        Factory_TreeNode_GitHub factory = newInstance(context);
        factory.setUser(context, user);
        factory.setRepository(context, repository);
        factory.setReference(context, reference);
        factory.setPath(context, path);
        return factory.create(context);
    }

    static public TreeNode tryConstruct (CallContext context, String user, String repository, String reference, String path) {
        Factory_TreeNode_GitHub factory = newInstance(context);
        factory.setUser(context, user);
        factory.setRepository(context, repository);
        factory.setReference(context, reference);
        factory.setPath(context, path);
        return factory.tryCreate(context);
    }

    public TreeNode create (CallContext context) throws ValidationFailure {
        TreeNode tn = this.tryCreate(context);
        if (tn == null) {
            ValidationFailure.createAndThrow(context, "No such tree node (github) '%(user)'/%(repository)'/%(reference)'/%(path)'", "path", this.getPath(context), "repository", this.getRepository(context), "reference", this.getReference(context), "path", this.getPath(context));
        }
        return tn;
    }

    public TreeNode tryCreate (CallContext context) {
        TreeNode node = TreeNode_GitHub.create(context, this.getUser(context), this.getRepository(context), this.getReference(context));
        String path = this.getPath(context);
        return path == null || path.isEmpty() ? node : node.tryGetChild(context, path);
    }

    public Object createObject    (CallContext context) throws ValidationFailure {
        return create(context);
    }

    public void   reset           (CallContext context) {
    }

    /* --------------------------------------------------------------------------------------------------------------------------------------- */

    protected String user;

    public String getUser (CallContext context) {
        return this.user;
    }

    public void setUser (CallContext context, String user) {
        this.user = user;
    }

    protected String repository;

    public String getRepository (CallContext context) {
        return this.repository;
    }

    public void setRepository (CallContext context, String repository) {
        this.repository = repository;
    }

    protected String reference;

    public String getReference (CallContext context) {
        return this.reference;
    }

    public void setReference (CallContext context, String reference) {
        this.reference = reference;
    }

    protected String path;

    public String getPath (CallContext context) {
        return this.path;
    }

    public void setPath (CallContext context, String path) {
        this.path = path;
    }

    /* --------------------------------------------------------------------------------------------------------------------------------------- */

    public void   validate        (CallContext context) throws ValidationFailure {
    }

    /* --------------------------------------------------------------------------------------------------------------------------------------- */

    public void confirmAttributes (CallContext context) {
    }

    public void validateFinally   (CallContext context) throws ValidationFailure {
    }
}
