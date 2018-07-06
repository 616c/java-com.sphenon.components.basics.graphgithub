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
import com.sphenon.basics.cache.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.locating.*;
import com.sphenon.basics.locating.locators.*;
import com.sphenon.basics.locating.returncodes.*;
import com.sphenon.basics.validation.returncodes.*;
import com.sphenon.basics.services.*;
import com.sphenon.basics.graph.*;
import com.sphenon.basics.graph.factories.*;

import com.sphenon.basics.graph.github.*;

public class SpecificFactoryTreeNodeByLocator_GitHub implements SpecificFactoryTreeNodeByLocator {

    public SpecificFactoryTreeNodeByLocator_GitHub(CallContext context) {
    }

    public void notifyNewConsumer(CallContext context, Consumer consumer) {
        // nice to see you
    }

    public boolean equals(Object object) {
        return (object instanceof SpecificFactoryTreeNodeByLocator_GitHub);
    }

    public String getLocatorClassId(CallContext context) {
        return "GitHub";
    }

    public TreeNode tryCreate(CallContext context, Locator locator) {
        LocatorGitHub lgh = (LocatorGitHub) locator;

        Locator.LocatorStep[] steps;

        try {
            steps = lgh.getLocatorSteps(context);
        } catch (InvalidLocator il) {
            CustomaryContext.create((Context)context).throwConfigurationError(context, il, "Github locator is invalid '%(locator)'", "locator", locator);
            throw (ExceptionConfigurationError) null; // compiler insists
        }

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
            
        TreeNode tn;
        try {
            tn = Factory_TreeNode_GitHub.construct(context, user, repository, reference, path);
        } catch (ValidationFailure vf) {
            CustomaryContext.create((Context)context).throwConfigurationError(context, vf, "Creation of TreeNode based on github locator failed '%(locator)'", "locator", locator);
            throw (ExceptionConfigurationError) null; // compiler insists
        }

        return tn;
    }
}
