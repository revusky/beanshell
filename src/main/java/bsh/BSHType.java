/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/



package bsh;

import java.lang.reflect.Array;

class BSHType extends SimpleNode implements BshClassManager.Listener {
    private static final long serialVersionUID = 1L;
    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    private Class<?> baseType;
    /**
        If we are an array type this will be non zero and indicate the
        dimensionality of the array.  e.g. 2 for String[][];
    */
    private int arrayDims;

    /**
        Internal cache of the type.  Cleared on classloader change.
    */
    private Class<?> type;

    String descriptor;

    BSHType(int id) {
        super(id);
    }

    /**
        Used by the grammar to indicate dimensions of array types
        during parsing.
    */
    public void addArrayDimension() {
        arrayDims++;
    }

    Node getTypeNode() {
        return jjtGetChild(0);
    }

    /**
         Returns a class descriptor for this type.
         If the type is an ambiguous name (object type) evaluation is
         attempted through the namespace in order to resolve imports.
         If it is not found and the name is non-compound we assume the default
         package for the name.
    */
    public String getTypeDescriptor(
        CallStack callstack, Interpreter interpreter, String defaultPackage )
    {
        // return cached type if available
        if ( descriptor != null )
            return descriptor;

        String descriptor;
        //  first node will either be PrimitiveType or AmbiguousName
        Node node = getTypeNode();
        if ( node instanceof BSHPrimitiveType )
            descriptor = getTypeDescriptor( ((BSHPrimitiveType)node).type );
        else
        {
            String clasName = ((BSHAmbiguousName)node).text;
            String innerClass = callstack.top().importedClasses.get(clasName);

            Class<?> clas = null;
            if ( innerClass == null ) try {
                clas = ((BSHAmbiguousName)node).toClass(
                    callstack, interpreter );
            } catch ( EvalError e ) {
                // Lets assume we have a generics raw type
                if (clasName.length() == 1)
                    clasName = "java.lang.Object";
            } else
                clasName = innerClass.replace('.', '$');

            if ( clas != null ) {
                descriptor = getTypeDescriptor( clas );
            } else {
                if ( defaultPackage == null || Name.isCompound( clasName ) )
                    descriptor = "L" + clasName.replace('.','/') + ";";
                else
                    descriptor =
                        "L"+defaultPackage.replace('.','/')+"/"+clasName + ";";
            }
        }

        for(int i=0; i<arrayDims; i++)
            descriptor = "["+descriptor;

        this.descriptor = descriptor;
        return descriptor;
    }

    public Class<?> getType( CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        // return cached type if available
        if ( type != null )
            return type;

        //  first node will either be PrimitiveType or AmbiguousName
        Node node = getTypeNode();
        if ( node instanceof BSHPrimitiveType )
            baseType = ((BSHPrimitiveType)node).getType();
        else
            try {
            baseType = ((BSHAmbiguousName)node).toClass(
                callstack, interpreter );
            } catch (EvalError e) {
                // Assuming generics raw type
                if (node.getText().trim().length() == 1
                        && e.getCause() instanceof ClassNotFoundException)
                    baseType = Object.class;
                else
                    throw e; // roll up unhandled error
            }

        if ( arrayDims > 0 ) {
            try {
                // Get the type by constructing a prototype array with
                // arbitrary (zero) length in each dimension.
                int[] dims = new int[arrayDims]; // int array default zeros
                Object obj = Array.newInstance(
                        null == baseType ? Object.class : baseType, dims);
                type = obj.getClass();
            } catch(Exception e) {
                throw new EvalError("Couldn't construct array type",
                    this, callstack, e);
            }
        } else
            type = baseType;

        // add listener to reload type if class is reloaded see #699
        interpreter.getClassManager().addListener(this);

        return type;
    }

    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    public Class<?> getBaseType() {
        return baseType;
    }
    /**
        If we are an array type this will be non zero and indicate the
        dimensionality of the array.  e.g. 2 for String[][];
    */
    public int getArrayDims() {
        return arrayDims;
    }

    /** Clear instance cache to reload types on class loader change #699 */
    public void classLoaderChanged() {
        type = null;
        baseType = null;
    }

    public static String getTypeDescriptor( Class<?> clas )
    {
        if ( clas == Boolean.TYPE ) return "Z";
        if ( clas == Character.TYPE ) return "C";
        if ( clas == Byte.TYPE ) return "B";
        if ( clas == Short.TYPE ) return "S";
        if ( clas == Integer.TYPE ) return "I";
        if ( clas == Long.TYPE ) return "J";
        if ( clas == Float.TYPE ) return "F";
        if ( clas == Double.TYPE ) return "D";
        if ( clas == Void.TYPE ) return "V";

        String name = clas.getName().replace('.','/');

        if ( name.startsWith("[") || name.endsWith(";") )
            return name;
        else
            return "L"+ name.replace('.','/') +";";
    }
}
