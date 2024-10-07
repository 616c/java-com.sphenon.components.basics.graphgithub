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
import com.sphenon.basics.context.classes.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.services.*;

import com.sphenon.basics.graph.github.factories.*;

public class GraphGitHubPackageInitialiser {

    static protected boolean initialised = false;

    static {
        initialise(RootContext.getRootContext());
    }

    static public void initialise (CallContext context) {
        
        if (initialised == false) {
            initialised = true;

            ServiceRegistry.registerService(context, new SpecificFactoryTreeNodeByLocator_GitHub(context));
        }
    }
}
