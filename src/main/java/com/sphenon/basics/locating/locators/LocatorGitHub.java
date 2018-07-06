package com.sphenon.basics.locating.locators;

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
import com.sphenon.basics.message.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.graph.*;
import com.sphenon.basics.validation.returncodes.*;
import com.sphenon.basics.locating.*;
import com.sphenon.basics.locating.factories.*;
import com.sphenon.basics.locating.returncodes.*;

import com.sphenon.basics.graph.*;
import com.sphenon.basics.graph.factories.*;
import com.sphenon.basics.graph.github.*;
import com.sphenon.basics.graph.github.factories.*;

import java.util.Vector;

public class LocatorGitHub extends Locator {

    public LocatorGitHub (CallContext context, String text_locator_value, Locator sub_locator, String locator_class_parameter_string) {
        super(context, text_locator_value, sub_locator, locator_class_parameter_string);
    }


    /* Parser States -------------------------------------------------------------------- */

    static protected LocatorParserState[] locator_parser_state;
        
    protected LocatorParserState[] getParserStates(CallContext context) {
        if (locator_parser_state == null) {
            locator_parser_state = new LocatorParserState[] {
                new LocatorParserState(context, "user"      , "user::String:1"      , false, false, null),
                new LocatorParserState(context, "repository", "repository::String:2", false, false, null),
                new LocatorParserState(context, "reference" , "reference::String:3" , false, true, Object.class),
                new LocatorParserState(context, "name"      , "name::String:3"      , false, true, Object.class),
            };
        }
        return locator_parser_state;
    }

    /* Base Acceptors ------------------------------------------------------------------- */

    static protected Vector<LocatorBaseAcceptor> locator_base_acceptors;

    static protected Vector<LocatorBaseAcceptor> initBaseAcceptors(CallContext context) {
        if (locator_base_acceptors == null) {
            locator_base_acceptors = new Vector<LocatorBaseAcceptor>();
        }
        return locator_base_acceptors;
    }

    protected Vector<LocatorBaseAcceptor> getBaseAcceptors(CallContext context) {
        return initBaseAcceptors(context);
    }

    static public void addBaseAcceptor(CallContext context, LocatorBaseAcceptor base_acceptor) {
        initBaseAcceptors(context).add(base_acceptor);
    }
    
    /* ---------------------------------------------------------------------------------- */

    public String getTargetVariableName(CallContext context) {
        return "resource";
    }

    protected Object retrieveLocalTarget(CallContext context, boolean create_java_code) throws InvalidLocator {
        // Object base = lookupBaseObject(context, true);

        if ((this.notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { CustomaryContext.create((Context)context).sendTrace(context, Notifier.SELF_DIAGNOSTICS, "Retrieving local target of GitHub Locator '%(textlocator)'...", "textlocator", this.text_locator_value); }

        LocatorStep[] steps = getLocatorSteps(context);

        String user       = steps[0].getValue(context);
        String repository = steps[1].getValue(context);
        String reference  = steps[2].getValue(context);
        String path       = null;

        for (int s=3; s<steps.length; s++) {
            if (path == null) {
                path = "";
            } else {
                path += "/";
            }
            path += steps[s].getValue(context);
        }

        try {
            TreeNode result = Factory_TreeNode_GitHub.construct(context, user, repository, reference, path);

            if (create_java_code) {
                this.javacode = new String[3];

                javacode[0] = "";
                javacode[1] = "com.sphenon.basics.graph.github.factories.Factory_TreeNode_GitHub.construct(context, \"" + user.replaceAll("\"","\\\\\"") + "\", \"" + repository.replaceAll("\"","\\\\\"") + "\", \"" + reference.replaceAll("\"","\\\\\"") + "\", \"" + path.replaceAll("\"","\\\\\"") + "\")";
                javacode[2] = "java.lang.Object";
            }

            return result;
        } catch (ValidationFailure vf) {
            CustomaryContext.create((Context)context).throwConfigurationError(context, vf, "Could not retrieve github resource referenced in locator '%(locator)')", "locator", this.text_locator_value);
            throw (ExceptionConfigurationError) null; // compiler insists
        }
    }

    protected String[] javacode;

    protected String[] getLocatorJavaCode(CallContext context) throws InvalidLocator {
        return javacode;
    }
}
