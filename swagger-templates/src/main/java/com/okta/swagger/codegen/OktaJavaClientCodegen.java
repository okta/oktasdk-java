/*
 * Copyright 2017 Okta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.swagger.codegen;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.languages.AbstractJavaCodegen;
import io.swagger.codegen.languages.features.BeanValidationFeatures;
import io.swagger.codegen.languages.features.GzipFeatures;
import io.swagger.codegen.languages.features.PerformBeanValidationFeatures;
import io.swagger.models.Swagger;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class OktaJavaClientCodegen extends AbstractJavaCodegen
        implements BeanValidationFeatures, PerformBeanValidationFeatures, GzipFeatures
{
    static final String MEDIA_TYPE = "mediaType";

    @SuppressWarnings("hiding")
    private static final Logger LOGGER = LoggerFactory.getLogger(OktaJavaClientCodegen.class);

    public static final String PARCELABLE_MODEL = "parcelableModel";

    protected boolean parcelableModel = false;
    protected boolean useBeanValidation = false;
    protected boolean performBeanValidation = false;
    protected boolean useGzipFeature = false;

    public OktaJavaClientCodegen() {
        super();
        outputFolder = "generated-code" + File.separator + "okta_java";
        embeddedTemplateDir = templateDir = "OktaJava";

        artifactId = "not_used";

        // TODO: these are hard coded for now, calling Maven Plugin does NOT set the packages correctly.
        invokerPackage = "com.okta.sdk.invoker";
        apiPackage = "com.okta.sdk.api";
        modelPackage = "com.okta.sdk.model";

        cliOptions.add(CliOption.newBoolean(PARCELABLE_MODEL, "Whether to generate models for Android that implement Parcelable with the okhttp-gson library."));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations"));
        cliOptions.add(CliOption.newBoolean(PERFORM_BEANVALIDATION, "Perform BeanValidation"));
        cliOptions.add(CliOption.newBoolean(USE_GZIP_FEATURE, "Send gzip-encoded requests"));

        supportedLibraries.put("okhttp-gson", "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.6.2. Enable Parcelable modles on Android using '-DparcelableModel=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.");

        CliOption libraryOption = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        libraryOption.setEnum(supportedLibraries);
        // set okhttp-gson as the default
        libraryOption.setDefault("okhttp-gson");
        cliOptions.add(libraryOption);
        setLibrary("okhttp-gson");

        apiTemplateFiles.clear();

        modelTemplateFiles.clear();
        modelTemplateFiles.put("modelInterface.mustache", ".java");
        modelTemplateFiles.put("modelImpl.mustache", "Impl.java");

    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        vendorExtensions.put("basePath", swagger.getBasePath());
        super.preprocessSwagger(swagger);
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + apiPackage().replace('.', File.separatorChar);
    }

    public String modelFileFolder() {
        return outputFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }


    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "javabrian";
    }

    @Override
    public String getHelp() {
        return "Generates a Java client library.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(PARCELABLE_MODEL)) {
            this.setParcelableModel(Boolean.valueOf(additionalProperties.get(PARCELABLE_MODEL).toString()));
        }
        // put the boolean value back to PARCELABLE_MODEL in additionalProperties
        additionalProperties.put(PARCELABLE_MODEL, parcelableModel);

        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            this.setUseBeanValidation(convertPropertyToBooleanAndWriteBack(USE_BEANVALIDATION));
        }

        if (additionalProperties.containsKey(PERFORM_BEANVALIDATION)) {
            this.setPerformBeanValidation(convertPropertyToBooleanAndWriteBack(PERFORM_BEANVALIDATION));
        }

        if (additionalProperties.containsKey(USE_GZIP_FEATURE)) {
            this.setUseGzipFeature(convertPropertyToBooleanAndWriteBack(USE_GZIP_FEATURE));
        }

        final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");
        final String authFolder = (sourceFolder + '/' + invokerPackage + ".auth").replace(".", "/");

        //Common files

        writeOptional(outputFolder, new SupportingFile("manifest.mustache", "", "AndroidManifest.xml"));

        if ("okhttp-gson".equals(getLibrary()) || StringUtils.isEmpty(getLibrary())) {
            // the "okhttp-gson" library template requires "ApiCallback.mustache" for async call
//            supportingFiles.add(new SupportingFile("ApiCallback.mustache", invokerFolder, "ApiCallback.java"));
//            supportingFiles.add(new SupportingFile("ApiResponse.mustache", invokerFolder, "ApiResponse.java"));
//            supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
//            supportingFiles.add(new SupportingFile("ProgressRequestBody.mustache", invokerFolder, "ProgressRequestBody.java"));
//            supportingFiles.add(new SupportingFile("ProgressResponseBody.mustache", invokerFolder, "ProgressResponseBody.java"));
//            supportingFiles.add(new SupportingFile("GzipRequestInterceptor.mustache", invokerFolder, "GzipRequestInterceptor.java"));
//            additionalProperties.put("gson", "true");
        } else {
            LOGGER.error("Unknown library option (-l/--library): " + getLibrary());
        }
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        return super.postProcessOperations(objs);
    }

    /**
     *  Prioritizes consumes mime-type list by moving json-vendor and json mime-types up front, but 
     *  otherwise preserves original consumes definition order. 
     *  [application/vnd...+json,... application/json, ..as is..]  
     *  
     * @param consumes consumes mime-type list
     * @return 
     */
    static List<Map<String, String>> prioritizeContentTypes(List<Map<String, String>> consumes) {
        if ( consumes.size() <= 1 )
            return consumes;
        
        List<Map<String, String>> prioritizedContentTypes = new ArrayList<>(consumes.size());
        
        List<Map<String, String>> jsonVendorMimeTypes = new ArrayList<>(consumes.size());
        List<Map<String, String>> jsonMimeTypes = new ArrayList<>(consumes.size());
        
        for ( Map<String, String> consume : consumes) {
            if ( isJsonVendorMimeType(consume.get(MEDIA_TYPE))) {
                jsonVendorMimeTypes.add(consume);
            }
            else if ( isJsonMimeType(consume.get(MEDIA_TYPE))) {
                jsonMimeTypes.add(consume);
            }
            else
                prioritizedContentTypes.add(consume);
            
            consume.put("hasMore", "true");
        }
        
        prioritizedContentTypes.addAll(0, jsonMimeTypes);
        prioritizedContentTypes.addAll(0, jsonVendorMimeTypes);
        
        prioritizedContentTypes.get(prioritizedContentTypes.size()-1).put("hasMore", null);
        
        return prioritizedContentTypes;
    }
    
    private static boolean isMultipartType(List<Map<String, String>> consumes) {
        Map<String, String> firstType = consumes.get(0);
        if (firstType != null) {
            if ("multipart/form-data".equals(firstType.get(MEDIA_TYPE))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        if(!BooleanUtils.toBoolean(model.isEnum)) {
            //final String lib = getLibrary();
            //Needed imports for Jackson based libraries
            if(additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonProperty");
            }
            if(additionalProperties.containsKey("gson")) {
                model.imports.add("SerializedName");
            }
        } else { // enum class
            //Needed imports for Jackson's JsonCreator
            if(additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonCreator");
            }
        }
    }

    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);
        //Needed import for Gson based libraries
        if (additionalProperties.containsKey("gson")) {
            List<Map<String, String>> imports = (List<Map<String, String>>)objs.get("imports");
            List<Object> models = (List<Object>) objs.get("models");
            for (Object _mo : models) {
                Map<String, Object> mo = (Map<String, Object>) _mo;
                CodegenModel cm = (CodegenModel) mo.get("model");
                // for enum model
                if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
                    cm.imports.add(importMapping.get("SerializedName"));
                    Map<String, String> item = new HashMap<String, String>();
                    item.put("import", importMapping.get("SerializedName"));
                    imports.add(item);
                }
            }
        }
        return objs;
    }

    public void setParcelableModel(boolean parcelableModel) {
        this.parcelableModel = parcelableModel;
    }

    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    public void setPerformBeanValidation(boolean performBeanValidation) {
        this.performBeanValidation = performBeanValidation;
    }

    public void setUseGzipFeature(boolean useGzipFeature) {
        this.useGzipFeature = useGzipFeature;
    }

    final private static Pattern JSON_MIME_PATTERN = Pattern.compile("(?i)application\\/json(;.*)?");
    final private static Pattern JSON_VENDOR_MIME_PATTERN = Pattern.compile("(?i)application\\/vnd.(.*)+json(;.*)?"); 

    /**
     * Check if the given MIME is a JSON MIME.
     * JSON MIME examples:
     *   application/json
     *   application/json; charset=UTF8
     *   APPLICATION/JSON
     */
    static boolean isJsonMimeType(String mime) {
        return mime != null && ( JSON_MIME_PATTERN.matcher(mime).matches());
    }

    /**
     * Check if the given MIME is a JSON Vendor MIME.
     * JSON MIME examples:
     *   application/vnd.mycompany+json
     *   application/vnd.mycompany.resourceA.version1+json
     */
    static boolean isJsonVendorMimeType(String mime) {
        return mime != null && JSON_VENDOR_MIME_PATTERN.matcher(mime).matches();
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }

        name = sanitizeName(name);
        return camelize(name) + "Api";
    }
}
