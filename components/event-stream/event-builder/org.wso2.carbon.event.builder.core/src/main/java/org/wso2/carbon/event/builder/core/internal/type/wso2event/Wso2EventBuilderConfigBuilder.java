/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.event.builder.core.internal.type.wso2event;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.event.builder.core.config.InputMapping;
import org.wso2.carbon.event.builder.core.exception.EventBuilderConfigurationException;
import org.wso2.carbon.event.builder.core.internal.config.InputMappingAttribute;
import org.wso2.carbon.event.builder.core.internal.util.EventBuilderConfigBuilder;
import org.wso2.carbon.event.builder.core.internal.util.EventBuilderConstants;
import org.wso2.carbon.event.builder.core.internal.util.EventBuilderUtil;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * This class is used to read the values of the event builder configuration defined in XML configuration files.
 * This class extends has methods to read syntax specific to Wso2EventInputMapping
 */
public class Wso2EventBuilderConfigBuilder {

    private static Wso2EventBuilderConfigBuilder instance = new Wso2EventBuilderConfigBuilder();

    private Wso2EventBuilderConfigBuilder() {

    }

    public static Wso2EventBuilderConfigBuilder getInstance() {
        return Wso2EventBuilderConfigBuilder.instance;
    }

    public InputMapping fromOM(
            OMElement mappingElement)
            throws EventBuilderConfigurationException {

        Wso2EventBuilderConfigBuilder.validateWso2EventMapping(mappingElement);
        Wso2EventInputMapping wso2EventInputMapping = new Wso2EventInputMapping();
        String customMappingEnabledAttribute = mappingElement.getAttributeValue(new QName(EventBuilderConstants.EB_ATTR_CUSTOM_MAPPING_ENABLED));
        if (customMappingEnabledAttribute == null || customMappingEnabledAttribute.equalsIgnoreCase(EventBuilderConstants.ENABLE_CONST)) {
            wso2EventInputMapping.setCustomMappingEnabled(true);
            Iterator propertyIterator = mappingElement.getChildrenWithName(new QName(EventBuilderConstants.EB_CONF_NS, EventBuilderConstants.EB_ELEMENT_PROPERTY));
            while (propertyIterator.hasNext()) {
                OMElement propertyOMElement = (OMElement) propertyIterator.next();
                InputMappingAttribute inputMappingAttribute = getInputMappingAttributeFromOM(propertyOMElement);
                wso2EventInputMapping.addInputMappingAttribute(inputMappingAttribute);
            }
            List<InputMappingAttribute> sortedInputMappingAttributes = EventBuilderUtil.sortInputMappingAttributes(wso2EventInputMapping.getInputMappingAttributes());
            int streamPosition = 0;
            for (InputMappingAttribute inputMappingAttribute : sortedInputMappingAttributes) {
                inputMappingAttribute.setToStreamPosition(streamPosition++);
            }
            wso2EventInputMapping.setInputMappingAttributes(sortedInputMappingAttributes);
        } else{
            wso2EventInputMapping.setCustomMappingEnabled(false);
        }

        return wso2EventInputMapping;
    }

    private InputMappingAttribute getInputMappingAttributeFromOM(OMElement omElement) {

        OMElement propertyFromElement = omElement.getFirstChildWithName(new QName(EventBuilderConstants.EB_CONF_NS, EventBuilderConstants.EB_ELEMENT_FROM));
        OMElement propertyToElement = omElement.getFirstChildWithName(new QName(EventBuilderConstants.EB_CONF_NS, EventBuilderConstants.EB_ELEMENT_TO));

        String name = propertyFromElement.getAttributeValue(new QName(EventBuilderConstants.EB_ATTR_NAME));
        String dataType = propertyFromElement.getAttributeValue(new QName(EventBuilderConstants.EB_ATTR_DATA_TYPE));
        String valueOf = propertyToElement.getAttributeValue(new QName(EventBuilderConstants.EB_ATTR_NAME));
        String attributeType = propertyToElement.getAttributeValue(new QName(EventBuilderConstants.EB_ATTR_TYPE));
        AttributeType type = EventBuilderConstants.STRING_ATTRIBUTE_TYPE_MAP.get(attributeType.toLowerCase());

        if (valueOf == null) {
            valueOf = name;
        }

        return new InputMappingAttribute(name, valueOf, type, dataType);
    }

    @SuppressWarnings("unchecked")
    public static void validateWso2EventMapping(OMElement omElement)
            throws EventBuilderConfigurationException {
        List<String> supportedChildTags = new ArrayList<String>();
        supportedChildTags.add(EventBuilderConstants.EB_ELEMENT_PROPERTY);

        String customMappingEnabledAttribute = omElement.getAttributeValue(new QName(EventBuilderConstants.EB_ATTR_CUSTOM_MAPPING_ENABLED));
        if (customMappingEnabledAttribute == null || customMappingEnabledAttribute.equalsIgnoreCase(EventBuilderConstants.ENABLE_CONST)) {

            int count = 0;
            Iterator<OMElement> mappingIterator = omElement.getChildElements();
            while (mappingIterator.hasNext()) {
                count++;
                OMElement childElement = mappingIterator.next();
                String childTag = childElement.getLocalName();
                if (!supportedChildTags.contains(childTag)) {
                    throw new EventBuilderConfigurationException("Unsupported XML configuration element for WSO2Event Input Mapping : " + childTag);
                }
                if (childTag.equals(EventBuilderConstants.EB_ELEMENT_PROPERTY)) {
                    OMElement propertyFromElement = childElement.getFirstChildWithName(new QName(EventBuilderConstants.EB_CONF_NS, EventBuilderConstants.EB_ELEMENT_FROM));
                    OMElement propertyToElement = childElement.getFirstChildWithName(new QName(EventBuilderConstants.EB_CONF_NS, EventBuilderConstants.EB_ELEMENT_TO));
                    if (propertyFromElement == null) {
                        throw new EventBuilderConfigurationException("An attribute mapping must provide a valid 'from' element");
                    }
                    if (propertyToElement == null) {
                        throw new EventBuilderConfigurationException("An attribute mapping must provide a valid 'to' element");
                    }
                    if (propertyToElement.getAttribute(new QName(EventBuilderConstants.EB_ATTR_NAME)) == null ||
                        propertyToElement.getAttribute(new QName(EventBuilderConstants.EB_ATTR_TYPE)) == null) {
                        throw new EventBuilderConfigurationException("An attribute mapping must provide name and type for its 'to' element.");
                    }
                }
            }

            if (count == 0) {
                throw new EventBuilderConfigurationException("There must be at least 1 attribute mapping with Custom Mapping enabled.");
            }
        }
    }

    public OMElement inputMappingToOM(
            InputMapping inputMapping, OMFactory factory) {

        Wso2EventInputMapping wso2EventInputMapping = (Wso2EventInputMapping) inputMapping;

        List<InputMappingAttribute> inputMappingAttributes = wso2EventInputMapping.getInputMappingAttributes();

        OMElement mappingOMElement = factory.createOMElement(new QName(EventBuilderConstants.EB_ELEMENT_MAPPING));
        mappingOMElement.declareDefaultNamespace(EventBuilderConstants.EB_CONF_NS);
        mappingOMElement.addAttribute(EventBuilderConstants.EB_ATTR_TYPE, EventBuilderConstants.EB_WSO2EVENT_MAPPING_TYPE, null);

        if (wso2EventInputMapping.isCustomMappingEnabled()) {
            mappingOMElement.addAttribute(EventBuilderConstants.EB_ATTR_CUSTOM_MAPPING_ENABLED, EventBuilderConstants.ENABLE_CONST, null);
        } else {
            mappingOMElement.addAttribute(EventBuilderConstants.EB_ATTR_CUSTOM_MAPPING_ENABLED, EventBuilderConstants.DISABLE_CONST, null);
        }
        if (inputMappingAttributes.size() > 0) {
            for (InputMappingAttribute inputMappingAttribute : inputMappingAttributes) {
                OMElement propertyOMElement = getPropertyOmElement(factory, inputMappingAttribute);
                propertyOMElement.setNamespace(mappingOMElement.getDefaultNamespace());
                mappingOMElement.addChild(propertyOMElement);
            }
        }

        return mappingOMElement;
    }

    protected OMElement getPropertyOmElement(OMFactory factory,
                                             InputMappingAttribute inputMappingAttribute) {
        OMElement propertyOMElement = factory.createOMElement(new QName(EventBuilderConstants.EB_ELEMENT_PROPERTY));

        OMElement fromElement = factory.createOMElement(new QName(EventBuilderConstants.EB_ELEMENT_FROM));
        fromElement.declareDefaultNamespace(EventBuilderConstants.EB_CONF_NS);
        fromElement.addAttribute(EventBuilderConstants.EB_ATTR_NAME, inputMappingAttribute.getFromElementKey(), null);
        fromElement.addAttribute(EventBuilderConstants.EB_ATTR_DATA_TYPE, inputMappingAttribute.getFromElementType(), null);

        OMElement toElement = factory.createOMElement(new QName(EventBuilderConstants.EB_ELEMENT_TO));
        toElement.declareDefaultNamespace(EventBuilderConstants.EB_CONF_NS);
        toElement.addAttribute(EventBuilderConstants.EB_ATTR_NAME, inputMappingAttribute.getToElementKey(), null);
        toElement.addAttribute(EventBuilderConstants.EB_ATTR_TYPE, EventBuilderConfigBuilder.getAttributeType(inputMappingAttribute.getToElementType()), null);

        propertyOMElement.addChild(fromElement);
        propertyOMElement.addChild(toElement);

        return propertyOMElement;
    }

}




