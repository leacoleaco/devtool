/*
 * Copyright (c) 2017.
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

package pro.leaco.mybatis.generator.plugins;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import pro.leaco.mybatis.generator.plugins.utils.*;
import pro.leaco.mybatis.generator.plugins.utils.hook.ISelectOneByExamplePluginHook;
import pro.leaco.mybatis.generator.plugins.utils.hook.ISelectSelectivePluginHook;

import java.util.List;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

/**
 */
public class SelectMapPlugin extends BasePlugin implements ISelectOneByExamplePluginHook {
    public static final String SELECT_BY_EXAMPLE_WHERE_CLAUSE = "Select_By_Example_Where_Clause";
    public static final String METHOD_SELECT_TO_MAP = "selectToMap";
    public static final String METHOD_SELECT_ONE_TO_MAP = "selectOneToMap";
    public static final String ID_FOR_PROPERTY_BASED_RESULT_MAP = "BasePropertyResultMap";
    private XmlElement select2MapEle;

    /**
     * 具体执行顺序 http://www.mybatis.org/generator/reference/pluggingIn.html
     *
     * @param warnings
     * @return
     */
    @Override
    public boolean validate(List<String> warnings) {

        // 插件使用前提是使用了ModelColumnPlugin插件
        if (!PluginTools.checkDependencyPlugin(getContext(), ModelColumnPlugin.class)) {
            warnings.add("itfsw:插件" + this.getClass().getTypeName() + "插件需配合com.fuyo.mybatis.generator.plugins.ModelColumnPlugin插件使用！");
            return false;
        }

        return super.validate(warnings);
    }

    /**
     * 具体执行顺序 http://www.mybatis.org/generator/reference/pluggingIn.html
     *
     * @param introspectedTable
     */
    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        super.initialized(introspectedTable);

        // bug:26,27
        this.select2MapEle = null;
    }

    // =========================================== client 方法生成 ===================================================

    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        FormatTools.addMethodWithBestPosition(interfaze, this.addSelectToMapMethod(
                method,
                METHOD_SELECT_TO_MAP,
                "@Param(\"example\")",
                introspectedTable,
                false));
        return super.clientSelectByExampleWithBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        if (!introspectedTable.hasBLOBColumns()) {
            FormatTools.addMethodWithBestPosition(interfaze, this.addSelectToMapMethod(
                    method,
                    METHOD_SELECT_TO_MAP,
                    "@Param(\"example\")",
                    introspectedTable,
                    false));
        }
        return super.clientSelectByExampleWithoutBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }
    // ============================================== sqlMap 生成 ===================================================


    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        this.select2MapEle = this.generateSelectToMapXmlElement(METHOD_SELECT_TO_MAP, introspectedTable, false);
        return super.sqlMapSelectByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        if (!introspectedTable.hasBLOBColumns()) {
            this.select2MapEle = this.generateSelectToMapXmlElement(METHOD_SELECT_TO_MAP, introspectedTable, false);
        }
        return super.sqlMapSelectByExampleWithBLOBsElementGenerated(element, introspectedTable);
    }


    /**
     * 具体执行顺序 http://www.mybatis.org/generator/reference/pluggingIn.html
     *
     * @param document
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        // issues#16
        if (introspectedTable.isConstructorBased()) {
            XmlElement resultMapEle = new XmlElement("resultMap");
            resultMapEle.addAttribute(new Attribute("id", ID_FOR_PROPERTY_BASED_RESULT_MAP));
            resultMapEle.addAttribute(new Attribute("type", introspectedTable.getRules().calculateAllFieldsClass().getFullyQualifiedName()));
            commentGenerator.addComment(resultMapEle);

            for (IntrospectedColumn introspectedColumn : introspectedTable.getPrimaryKeyColumns()) {
                resultMapEle.addElement(XmlElementGeneratorTools.generateResultMapResultElement("id", introspectedColumn));
            }
            for (IntrospectedColumn introspectedColumn : introspectedTable.getNonPrimaryKeyColumns()) {
                resultMapEle.addElement(XmlElementGeneratorTools.generateResultMapResultElement("result", introspectedColumn));
            }
            document.getRootElement().addElement(0, resultMapEle);
        }

        //0. 生成专用 where clause
        FormatTools.addElementWithBestPosition(document.getRootElement(), this.generateSelectByExampleWhereClauseElement(introspectedTable));

        // 1. selectByExampleSelective 方法
        if (this.select2MapEle != null) {
            FormatTools.addElementWithBestPosition(document.getRootElement(), this.select2MapEle);
            PluginTools.getHook(ISelectSelectivePluginHook.class).sqlMapSelectByExampleSelectiveElementGenerated(document, this.select2MapEle, introspectedTable);
        }


        return true;
    }

    // ===================================== ISelectOneByExamplePluginHook =========================================

    @Override
    public boolean clientSelectOneByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        FormatTools.addMethodWithBestPosition(interfaze, this.addSelectToMapMethod(
                method,
                METHOD_SELECT_ONE_TO_MAP,
                "@Param(\"example\")",
                introspectedTable,
                true));
        return true;
    }

    @Override
    public boolean clientSelectOneByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        if (!introspectedTable.hasBLOBColumns()) {
            FormatTools.addMethodWithBestPosition(interfaze, this.addSelectToMapMethod(
                    method,
                    METHOD_SELECT_ONE_TO_MAP,
                    "@Param(\"example\")",
                    introspectedTable,
                    true));
        }
        return true;
    }

    @Override
    public boolean sqlMapSelectOneByExampleWithoutBLOBsElementGenerated(Document document, XmlElement element, IntrospectedTable introspectedTable) {

        if (!introspectedTable.hasBLOBColumns()) {
            FormatTools.addElementWithBestPosition(document.getRootElement(), this.generateSelectOneByExampleSelectiveElement(introspectedTable));
        }
        return true;
    }


    @Override
    public boolean sqlMapSelectOneByExampleWithBLOBsElementGenerated(Document document, XmlElement element, IntrospectedTable introspectedTable) {
        FormatTools.addElementWithBestPosition(document.getRootElement(), this.generateSelectOneByExampleSelectiveElement(introspectedTable));
        return true;
    }

    // =========================================== 一些私有方法 =====================================================

    /**
     * 生成selectOneByExampleSelective
     *
     * @param introspectedTable
     * @return
     */
    private XmlElement generateSelectOneByExampleSelectiveElement(IntrospectedTable introspectedTable) {
        return this.generateSelectToMapXmlElement(METHOD_SELECT_ONE_TO_MAP, introspectedTable, true);
    }

    private XmlElement generateSelectByExampleWhereClauseElement(IntrospectedTable introspectedTable) {
        XmlElement element = new XmlElement("sql");
        commentGenerator.addComment(element);
        element.addAttribute(new Attribute("id", SELECT_BY_EXAMPLE_WHERE_CLAUSE));

        XmlElement where = new XmlElement("where");
        element.addElement(where);

        XmlElement forEachEle = XmlElementTools.createElement(
                "foreach",
                new Attribute("collection", "example.oredCriteria"),
                new Attribute("item", "criteria"),
                new Attribute("separator", "or"),
                () -> XmlElementTools.createElement(
                        "if",
                        new Attribute("test", "criteria.valid"),
                        () -> XmlElementTools.createElement(
                                "trim",
                                new Attribute("prefix", "("),
                                new Attribute("prefixOverrides", "and"),
                                new Attribute("suffix", ")"),
                                () -> XmlElementTools.createElement(
                                        "foreach",
                                        new Attribute("collection", "criteria.criteria"),
                                        new Attribute("item", "criterion"),
                                        () -> XmlElementTools.createElement(
                                                "choose",
                                                () -> XmlElementTools.createElement(
                                                        "when",
                                                        new Attribute("test", "criterion.noValue"),
                                                        () -> new TextElement("and ${criterion.condition}")
                                                ),
                                                () -> XmlElementTools.createElement(
                                                        "when",
                                                        new Attribute("test", "criterion.singleValue"),
                                                        () -> new TextElement("and ${criterion.condition} #{criterion.value}")
                                                ),
                                                () -> XmlElementTools.createElement(
                                                        "when",
                                                        new Attribute("test", "criterion.betweenValue"),
                                                        () -> new TextElement("and ${criterion.condition} #{criterion.value} and #{criterion.secondValue}")
                                                ),
                                                () -> XmlElementTools.createElement(
                                                        "when",
                                                        new Attribute("test", "criterion.listValue"),
                                                        () -> new TextElement("and ${criterion.condition}"),
                                                        () -> XmlElementTools.createElement(
                                                                "foreach",
                                                                new Attribute("close", ")"),
                                                                new Attribute("collection", "criterion.value"),
                                                                new Attribute("item", "listItem"),
                                                                new Attribute("open", "("),
                                                                new Attribute("separator", ","),
                                                                () -> new TextElement("#{listItem}")
                                                        )
                                                )
                                        )
                                )
                        ))
        );

        where.addElement(forEachEle);


        XmlElement joinCriteriaEle =
                XmlElementTools.createElement(
                        "if",
                        new Attribute("test", "example.joinCriteria!=null and example.joinCriteria.size()>0"),
                        () -> XmlElementTools.createElement(
                                "foreach",
                                new Attribute("collection", "example.joinCriteria"),
                                new Attribute("item", "criteria"),
                                () -> XmlElementTools.createElement(
                                        "if",
                                        new Attribute("test", "criteria.valid"),
                                        () -> XmlElementTools.createElement(
                                                "trim",
                                                new Attribute("prefix", "and ("),
                                                new Attribute("prefixOverrides", "and"),
                                                new Attribute("suffix", ")"),
                                                () -> XmlElementTools.createElement(
                                                        "foreach",
                                                        new Attribute("collection", "criteria.criteria"),
                                                        new Attribute("item", "criterion"),
                                                        () -> XmlElementTools.createElement(
                                                                "choose",
                                                                () -> XmlElementTools.createElement(
                                                                        "when",
                                                                        new Attribute("test", "criterion.noValue"),
                                                                        () -> new TextElement("and ${criterion.condition}")
                                                                ),
                                                                () -> XmlElementTools.createElement(
                                                                        "when",
                                                                        new Attribute("test", "criterion.singleValue"),
                                                                        () -> new TextElement("and ${criterion.condition} #{criterion.value}")
                                                                ),
                                                                () -> XmlElementTools.createElement(
                                                                        "when",
                                                                        new Attribute("test", "criterion.betweenValue"),
                                                                        () -> new TextElement("and ${criterion.condition} #{criterion.value} and #{criterion.secondValue}")
                                                                ),
                                                                () -> XmlElementTools.createElement(
                                                                        "when",
                                                                        new Attribute("test", "criterion.listValue"),
                                                                        () -> new TextElement("and ${criterion.condition}"),
                                                                        () -> XmlElementTools.createElement(
                                                                                "foreach",
                                                                                new Attribute("close", ")"),
                                                                                new Attribute("collection", "criterion.value"),
                                                                                new Attribute("item", "listItem"),
                                                                                new Attribute("open", "("),
                                                                                new Attribute("separator", ","),
                                                                                () -> new TextElement("#{listItem}")
                                                                        )
                                                                )
                                                        )
                                                )
                                        ))

                        )
                );

        where.addElement(joinCriteriaEle);

        return element;
    }


    /**
     * 生成selectToMap
     * Mapper 对象
     *
     * @param introspectedTable
     * @return
     */
    private XmlElement generateSelectToMapXmlElement(String id, IntrospectedTable introspectedTable, boolean selectOne) {
        XmlElement selectSelectiveEle = new XmlElement("select");
        commentGenerator.addComment(selectSelectiveEle);

        selectSelectiveEle.addAttribute(new Attribute("id", id));
        selectSelectiveEle.addAttribute(new Attribute("resultType", "java.util.Map"));
        selectSelectiveEle.addAttribute(new Attribute("parameterType", "map"));


        //BY DISTINCT FIRST
        selectSelectiveEle.addElement(new TextElement("select"));
        if (!selectOne) {
            // issues#20
            XmlElement ifDistinctElement = new XmlElement("if");
            ifDistinctElement.addAttribute(new Attribute("test", "example != null and example.distinct"));
            ifDistinctElement.addElement(new TextElement("distinct"));
            selectSelectiveEle.addElement(ifDistinctElement);
        }

        //issue#102
        if (stringHasValue(introspectedTable.getSelectByExampleQueryId())) {
            selectSelectiveEle.addElement(new TextElement("'" + introspectedTable.getSelectByExampleQueryId() + "' as QUERYID,"));
        }
        //BY DISTINCT END


        //FORM
        selectSelectiveEle.addElement(this.generateSelectiveElement(introspectedTable));
        selectSelectiveEle.addElement(new TextElement("from " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));

        // JOIN
        XmlElement ifJoinElement = new XmlElement("if");
        ifJoinElement.addAttribute(new Attribute("test", "example != null and example.joinClause != null"));
        ifJoinElement.addElement(new TextElement("${example.joinClause}"));
        selectSelectiveEle.addElement(ifJoinElement);
        // JOIN END

        // WHERE START
        XmlElement ifElement = new XmlElement("if");
        ifElement.addAttribute(new Attribute("test", "example != null"));

        XmlElement includeElement = new XmlElement("include");
        includeElement.addAttribute(new Attribute("refid", SELECT_BY_EXAMPLE_WHERE_CLAUSE));
        ifElement.addElement(includeElement);

        selectSelectiveEle.addElement(ifElement);
        // WHERE END

        //group by
        XmlElement ifGroupByElement = new XmlElement("if");
        ifGroupByElement.addAttribute(new Attribute("test", "example != null and example.groupByClause != null"));
        ifGroupByElement.addElement(new TextElement("group by ${example.groupByClause}"));
        selectSelectiveEle.addElement(ifGroupByElement);

        //order by
        XmlElement ifElement1 = new XmlElement("if");
        ifElement1.addAttribute(new Attribute("test", "example != null and example.orderByClause != null"));
        ifElement1.addElement(new TextElement("order by ${example.orderByClause}"));
        selectSelectiveEle.addElement(ifElement1);
        //BY EXAMPLE END

        if (selectOne) {
            // 只查询一条
            selectSelectiveEle.addElement(new TextElement("limit 1"));
        }

        return selectSelectiveEle;
    }


    /**
     * 生成Selective xml节点
     *
     * @param introspectedTable
     * @return
     */
    private XmlElement generateSelectiveElement(IntrospectedTable introspectedTable) {
        XmlElement chooseEle = new XmlElement("choose");

        XmlElement whenEle = new XmlElement("when");
        whenEle.addAttribute(new Attribute("test", "expressions != null and expressions.length > 0"));
        chooseEle.addElement(whenEle);

        // 生成返回字段节点
        XmlElement keysEle = new XmlElement("foreach");
        whenEle.addElement(keysEle);
        keysEle.addAttribute(new Attribute("collection", "expressions"));
        keysEle.addAttribute(new Attribute("item", "expression"));
        keysEle.addAttribute(new Attribute("separator", ","));
//        keysEle.addElement(new TextElement("${column.aliasedEscapedColumnName}"));
        keysEle.addElement(new TextElement("${expression}"));

//        XmlElement otherwiseEle = new XmlElement("otherwise");
//        chooseEle.addElement(otherwiseEle);
//        // 存在关键词column或者table定义了alias属性,这里直接使用对应的ColumnListElement
//        if (introspectedTable.getRules().generateSelectByExampleWithBLOBs()) {
//            otherwiseEle.addElement(XmlElementGeneratorTools.getBaseColumnListElement(introspectedTable));
//            otherwiseEle.addElement(new TextElement(","));
//            otherwiseEle.addElement(XmlElementGeneratorTools.getBlobColumnListElement(introspectedTable));
//        } else {
//            otherwiseEle.addElement(XmlElementGeneratorTools.getBaseColumnListElement(introspectedTable));
//        }

        return chooseEle;
    }

    /**
     * 生成 生成到方法
     *
     * @param method
     * @param name
     * @param firstAnnotation
     * @param introspectedTable
     * @param selectOne
     * @return
     */
    private Method addSelectToMapMethod(Method method, String name, String firstAnnotation, IntrospectedTable introspectedTable, boolean selectOne) {
        Method method1 = JavaElementTools.clone(method);
        FormatTools.replaceGeneralMethodComment(commentGenerator, method1, introspectedTable);

        method1.setName(name);
        // example
        method1.getParameters().get(0).addAnnotation(firstAnnotation);
        // selective
        method1.addParameter(
                new Parameter(FullyQualifiedJavaType.getStringInstance(),
                        "expressions", "@Param(\"expressions\")",
                        true)
        );

        if (selectOne) {
            method1.setReturnType(new FullyQualifiedJavaType("java.util.Map"));
        } else {
            method1.setReturnType(new FullyQualifiedJavaType("java.util.List<java.util.Map>"));
        }

        return method1;
    }

    /**
     * 获取ModelColumn type
     *
     * @param introspectedTable
     * @return
     */
    private FullyQualifiedJavaType getModelColumnFullyQualifiedJavaType(IntrospectedTable introspectedTable) {
        return new FullyQualifiedJavaType(introspectedTable.getRules().calculateAllFieldsClass().getShortName() + "." + ModelColumnPlugin.ENUM_NAME);
    }
}
