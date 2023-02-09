package pro.leaco.mybatis.generator.plugins;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import pro.leaco.mybatis.generator.plugins.utils.BasePlugin;
import pro.leaco.mybatis.generator.plugins.utils.FormatTools;
import pro.leaco.mybatis.generator.plugins.utils.JavaElementGeneratorTools;
import pro.leaco.mybatis.generator.plugins.utils.PluginTools;

import java.util.List;

/**
 * ---------------------------------------------------------------------------
 * Example 增强插件
 * ---------------------------------------------------------------------------
 *
 * 
 * 
 * ---------------------------------------------------------------------------
 */
public class ExampleEnhancedPlugin extends BasePlugin {
    // newAndCreateCriteria 方法
    public static final String METHOD_NEW_AND_CREATE_CRITERIA = "newAndCreateCriteria";
    // 逻辑删除列-Key
    public static final String PRO_ENABLE_AND_IF = "enableAndIf";
    // 是否启用column的操作
    private boolean enableColumnOperate = false;

    /**
     * {@inheritDoc}
     *
     * @param introspectedTable
     */
    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        super.initialized(introspectedTable);
        this.enableColumnOperate = PluginTools.checkDependencyPlugin(context, ModelColumnPlugin.class);
        String enableAndIf = properties.getProperty(PRO_ENABLE_AND_IF);
    }

    /**
     * ModelExample Methods 生成
     * 具体执行顺序 http://www.mybatis.org/generator/reference/pluggingIn.html
     *
     * @param topLevelClass
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        List<InnerClass> innerClasses = topLevelClass.getInnerClasses();
        for (int i = 0; i < innerClasses.size(); i++) {
            InnerClass innerClass = innerClasses.get(i);
            if ("Criteria".equals(innerClass.getType().getShortName())) {
                // 工厂方法
                addFactoryMethodToCriteria(topLevelClass, innerClass, introspectedTable);
                // when
                addWhenToCriteria(topLevelClass, innerClass, introspectedTable);
            } else if ("GeneratedCriteria".equals(innerClass.getType().getShortName())) {
                //增加自定义condition where 方法
                addCustomConditionMethodToCriteria(topLevelClass, innerClass, introspectedTable);
                // 增加 column 操作方法
                if (this.enableColumnOperate) {
                    addColumnMethodToCriteria(topLevelClass, innerClass, introspectedTable);
                }
            }
        }

        List<Method> methods = topLevelClass.getMethods();
        for (Method method : methods) {
            if (!"createCriteriaInternal".equals(method.getName())) {
                continue;
            } else {
                method.getBodyLines().set(0, "Criteria criteria = new Criteria(this);");
                logger.debug("itfsw(Example增强插件):" + topLevelClass.getType().getShortName() + "修改createCriteriaInternal方法，修改构造Criteria时传入Example对象");
            }
        }

        // and criteria 方法
        this.addAddMethodToExample(topLevelClass, introspectedTable);

        // join 方法
        this.addjoinFieldToExample(topLevelClass, introspectedTable);
        this.addJoinMethodToExample(topLevelClass, introspectedTable);

        // groupBy方法
        this.addGroupByMethodToExample(topLevelClass, introspectedTable);

        // orderBy方法
        this.addOrderByMethodToExample(topLevelClass, introspectedTable);

        // createCriteria 静态方法
        this.addStaticCreateCriteriaMethodToExample(topLevelClass, introspectedTable);

        // when
        addWhenToExample(topLevelClass, introspectedTable);

        return true;
    }

    private void addjoinFieldToExample(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // TODO: 以后可以换统一的接口， 而不是 Object
        FullyQualifiedJavaType joinCriteriaType = new FullyQualifiedJavaType("java.util.List<Object>");
        Field joinCriteria = new Field("joinCriteria", joinCriteriaType);
        joinCriteria.setVisibility(JavaVisibility.PROTECTED);
        commentGenerator.addFieldComment(joinCriteria, introspectedTable);
        topLevelClass.addField(joinCriteria);

        Method joinCriteriaMethod = JavaElementGeneratorTools.generateMethod(
                "getJoinCriteria",
                false, JavaVisibility.PUBLIC,
                joinCriteriaType
        );
        commentGenerator.addGeneralMethodComment(joinCriteriaMethod, introspectedTable);
        joinCriteriaMethod.addBodyLine("return this.joinCriteria;");

        FormatTools.addMethodWithBestPosition(topLevelClass, joinCriteriaMethod);
    }

    private void addAddMethodToExample(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        Method joinCriteriaMethod = JavaElementGeneratorTools.generateMethod(
                "andCriteria",
                false, JavaVisibility.PUBLIC,
                new FullyQualifiedJavaType(topLevelClass.getType().getShortName()),
                new Parameter(FullyQualifiedJavaType.getObjectInstance(), "criteria")
        );
        commentGenerator.addGeneralMethodComment(joinCriteriaMethod, introspectedTable);

//        joinCriteriaMethod.addBodyLine(topLevelClass.getType().getShortName() + " example = new " + topLevelClass.getType().getShortName() + "();");
        joinCriteriaMethod.addBodyLine("if(this.joinCriteria == null){");
        joinCriteriaMethod.addBodyLine("this.joinCriteria = new java.util.ArrayList<>();");
        joinCriteriaMethod.addBodyLine("}");
        joinCriteriaMethod.addBodyLine("this.joinCriteria.add(criteria);");
        joinCriteriaMethod.addBodyLine("return this;");

        FormatTools.addMethodWithBestPosition(topLevelClass, joinCriteriaMethod);

    }

    /**
     * 添加 createCriteria 静态方法
     *
     * @param topLevelClass
     * @param introspectedTable
     */
    private void addStaticCreateCriteriaMethodToExample(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        Method createCriteriaMethod = JavaElementGeneratorTools.generateMethod(
                METHOD_NEW_AND_CREATE_CRITERIA,
                false, JavaVisibility.PUBLIC,
                FullyQualifiedJavaType.getCriteriaInstance()
        );
        commentGenerator.addGeneralMethodComment(createCriteriaMethod, introspectedTable);

        createCriteriaMethod.setStatic(true);
        createCriteriaMethod.addBodyLine(topLevelClass.getType().getShortName() + " example = new " + topLevelClass.getType().getShortName() + "();");
        createCriteriaMethod.addBodyLine("return example.createCriteria();");

        FormatTools.addMethodWithBestPosition(topLevelClass, createCriteriaMethod);
    }


    /**
     * 添加自定义where条件操作方法
     *
     * @param topLevelClass
     * @param innerClass
     * @param introspectedTable
     */
    private void addCustomConditionMethodToCriteria(TopLevelClass topLevelClass, InnerClass innerClass, IntrospectedTable introspectedTable) {
        // !!!!! Column import比较特殊引入的是外部类
        FormatTools.addMethodWithBestPosition(innerClass, this.generateCustomConditionMethod());
        FormatTools.addMethodWithBestPosition(innerClass, this.generateCustomAndConditionMethod("andBetween", "between"));
        FormatTools.addMethodWithBestPosition(innerClass, this.generateCustomAndConditionMethod("andNotBetween", "not between"));
    }

    /**
     * 生成自定义where条件具体方法
     */
    private Method generateCustomConditionMethod() {
        // 方法名
        Method method = JavaElementGeneratorTools.generateMethod(
                "and",
                false, JavaVisibility.PUBLIC,
                FullyQualifiedJavaType.getCriteriaInstance(),
                new Parameter(
                        FullyQualifiedJavaType.getStringInstance(),
                        "sql",
                        true
                )
        );

        // 方法体
        JavaElementGeneratorTools.generateMethodBody(
                method,
                "String condition = \"\";",
                "for (String s : sql) {",
                "condition += s + \" \";",
                "}",
                "addCriterion(condition);",
                "return (Criteria) this;"
        );

        return method;
    }

    /**
     * 生成自定义where条件具体方法
     */
    private Method generateCustomAndConditionMethod(String methodName, String condition) {
        // 方法名
        Method method = JavaElementGeneratorTools.generateMethod(
                methodName,
                false, JavaVisibility.PUBLIC,
                FullyQualifiedJavaType.getCriteriaInstance(),
                new Parameter(FullyQualifiedJavaType.getStringInstance(), "exp"),
                new Parameter(FullyQualifiedJavaType.getObjectInstance(), "value1"),
                new Parameter(FullyQualifiedJavaType.getObjectInstance(), "value2")
        );

        // 方法体
        JavaElementGeneratorTools.generateMethodBody(
                method,
                "addCriterion(exp + \" " + condition + "\" , value1, value2, exp );",
                "return (Criteria) this;"
        );
        return method;
    }


    /**
     * 添加列操作方法
     *
     * @param topLevelClass
     * @param innerClass
     * @param introspectedTable
     */
    private void addColumnMethodToCriteria(TopLevelClass topLevelClass, InnerClass innerClass, IntrospectedTable introspectedTable) {
        // !!!!! Column import比较特殊引入的是外部类
        topLevelClass.addImportedType(introspectedTable.getRules().calculateAllFieldsClass());
        for (IntrospectedColumn introspectedColumn : introspectedTable.getNonBLOBColumns()) {
            topLevelClass.addImportedType(introspectedColumn.getFullyQualifiedJavaType());
            // EqualTo
            FormatTools.addMethodWithBestPosition(innerClass, this.generateSingleValueMethod(introspectedTable, introspectedColumn, "EqualTo", "="));
            // NotEqualTo
            FormatTools.addMethodWithBestPosition(innerClass, this.generateSingleValueMethod(introspectedTable, introspectedColumn, "NotEqualTo", "<>"));
            // GreaterThan
            FormatTools.addMethodWithBestPosition(innerClass, this.generateSingleValueMethod(introspectedTable, introspectedColumn, "GreaterThan", ">"));
            // GreaterThanOrEqualTo
            FormatTools.addMethodWithBestPosition(innerClass, this.generateSingleValueMethod(introspectedTable, introspectedColumn, "GreaterThanOrEqualTo", ">="));
            // LessThan
            FormatTools.addMethodWithBestPosition(innerClass, this.generateSingleValueMethod(introspectedTable, introspectedColumn, "LessThan", "<"));
            // LessThanOrEqualTo
            FormatTools.addMethodWithBestPosition(innerClass, this.generateSingleValueMethod(introspectedTable, introspectedColumn, "LessThanOrEqualTo", "<="));
        }
    }

    /**
     * 生成column操作的具体方法
     *
     * @param introspectedTable
     * @param introspectedColumn
     * @param nameFragment
     * @param operator
     * @return
     */
    private Method generateSingleValueMethod(IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn, String nameFragment, String operator) {
        // 方法名
        StringBuilder sb = new StringBuilder();
        sb.append(introspectedColumn.getJavaProperty());
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        sb.insert(0, "and");
        sb.append(nameFragment);
        sb.append("Column");

        Method method = JavaElementGeneratorTools.generateMethod(
                sb.toString(),
                false, JavaVisibility.PUBLIC,
                FullyQualifiedJavaType.getCriteriaInstance(),
                new Parameter(
                        new FullyQualifiedJavaType(introspectedTable.getRules().calculateAllFieldsClass().getShortName() + "." + ModelColumnPlugin.ENUM_NAME),
                        "column"
                )
        );

        // 方法体
        sb.setLength(0);
        sb.append("addCriterion(\"");
        sb.append(MyBatis3FormattingUtilities.getAliasedActualColumnName(introspectedColumn));
        sb.append(" ");
        sb.append(operator);
        sb.append(" \" + ");
        sb.append("column.");
        sb.append(ModelColumnPlugin.METHOD_GET_ESCAPED_COLUMN_NAME);
        sb.append("());");

        JavaElementGeneratorTools.generateMethodBody(
                method,
                sb.toString(),
                "return (Criteria) this;"
        );

        return method;
    }

    /**
     * 添加工厂方法
     *
     * @param topLevelClass
     * @param innerClass
     * @param introspectedTable
     */
    private void addFactoryMethodToCriteria(TopLevelClass topLevelClass, InnerClass innerClass, IntrospectedTable introspectedTable) {
        // example field
        Field exampleField = JavaElementGeneratorTools.generateField(
                "example",
                JavaVisibility.PRIVATE,
                topLevelClass.getType(),
                null
        );
        commentGenerator.addFieldComment(exampleField, introspectedTable);
        innerClass.addField(exampleField);

        // overwrite constructor
        List<Method> methods = innerClass.getMethods();
        for (Method method : methods) {
            if (method.isConstructor()) {
                method.addParameter(new Parameter(topLevelClass.getType(), "example"));
                method.addBodyLine("this.example = example;");
                commentGenerator.addGeneralMethodComment(method, introspectedTable);
                logger.debug("itfsw(Example增强插件):" + topLevelClass.getType().getShortName() + "修改构造方法，增加example参数");
            }
        }

        // 添加example工厂方法
        Method exampleMethod = JavaElementGeneratorTools.generateMethod(
                "example",
                false, JavaVisibility.PUBLIC,
                topLevelClass.getType()
        );
        commentGenerator.addGeneralMethodComment(exampleMethod, introspectedTable);
        exampleMethod = JavaElementGeneratorTools.generateMethodBody(
                exampleMethod,
                "return this.example;"
        );
        FormatTools.addMethodWithBestPosition(innerClass, exampleMethod);
        logger.debug("itfsw(Example增强插件):" + topLevelClass.getType().getShortName() + "." + innerClass.getType().getShortName() + "增加工厂方法example");
    }

    /**
     * 增强Criteria的链式调用(when)
     *
     * @param topLevelClass
     * @param innerClass
     * @param introspectedTable
     */
    private void addWhenToCriteria(TopLevelClass topLevelClass, InnerClass innerClass, IntrospectedTable introspectedTable) {
        this.addWhenToClass(topLevelClass, innerClass, introspectedTable, "criteria");
    }

    /**
     * 增强Example的链式调用(when)
     *
     * @param topLevelClass
     * @param introspectedTable
     */
    private void addWhenToExample(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        this.addWhenToClass(topLevelClass, topLevelClass, introspectedTable, "example");
    }

    /**
     * 增强链式调用(when)
     *
     * @param topLevelClass
     * @param clazz
     * @param introspectedTable
     */
    private void addWhenToClass(TopLevelClass topLevelClass, InnerClass clazz, IntrospectedTable introspectedTable, String type) {
        // 添加接口When
        InnerInterface whenInterface = new InnerInterface("I" + FormatTools.upFirstChar(type) + "When");
        whenInterface.setVisibility(JavaVisibility.PUBLIC);

        // ICriteriaAdd增加接口add
        Method addMethod = JavaElementGeneratorTools.generateMethod(
                type,
                false, JavaVisibility.DEFAULT,
                null,
                new Parameter(clazz.getType(), type)
        );
        commentGenerator.addGeneralMethodComment(addMethod, introspectedTable);
        addMethod.setAbstract(true);
        whenInterface.addMethod(addMethod);

//        InnerClass innerClassWrapper = new InnerInterfaceWrapperToInnerClass(whenInterface);
        // 添加注释
//        commentGenerator.addClassComment(whenInterface, introspectedTable);
        topLevelClass.addInnerInterface(whenInterface);

        // 添加when方法
        Method whenMethod = JavaElementGeneratorTools.generateMethod(
                "when",
                false, JavaVisibility.PUBLIC,
                clazz.getType(),
                new Parameter(FullyQualifiedJavaType.getBooleanPrimitiveInstance(), "condition"),
                new Parameter(whenInterface.getType(), "then")
        );
        commentGenerator.addGeneralMethodComment(whenMethod, introspectedTable);
        whenMethod = JavaElementGeneratorTools.generateMethodBody(
                whenMethod,
                "if (condition) {",
                "then." + type + "(this);",
                "}",
                "return this;"
        );
        FormatTools.addMethodWithBestPosition(clazz, whenMethod);
        Method whenOtherwiseMethod = JavaElementGeneratorTools.generateMethod(
                "when",
                false, JavaVisibility.PUBLIC,
                clazz.getType(),
                new Parameter(FullyQualifiedJavaType.getBooleanPrimitiveInstance(), "condition"),
                new Parameter(whenInterface.getType(), "then"),
                new Parameter(whenInterface.getType(), "otherwise")
        );
        commentGenerator.addGeneralMethodComment(whenOtherwiseMethod, introspectedTable);
        whenOtherwiseMethod = JavaElementGeneratorTools.generateMethodBody(
                whenOtherwiseMethod,
                "if (condition) {",
                "then." + type + "(this);",
                "} else {",
                "otherwise." + type + "(this);",
                "}",
                "return this;"
        );
        FormatTools.addMethodWithBestPosition(clazz, whenOtherwiseMethod);
    }

    /**
     * Example新增 leftJoin(String table,String condition)方法直接返回example，增强链式调用，可以一路.下去了。
     *
     * @param topLevelClass
     * @param introspectedTable
     */
    private void addJoinMethodToExample(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // 添加 joinClause
        Field joinClauseField = JavaElementGeneratorTools.generateField(
                "joinClause",
                JavaVisibility.PROTECTED,
                new FullyQualifiedJavaType("java.lang.String"),
                null
        );
        commentGenerator.addFieldComment(joinClauseField, introspectedTable);
        topLevelClass.addField(joinClauseField);


        // 添加 getJoinClause()
        Method getMethod = JavaElementGeneratorTools.generateMethod(
                "getJoinClause",
                false, JavaVisibility.PUBLIC,
                new FullyQualifiedJavaType("java.lang.String")
        );
        commentGenerator.addGeneralMethodComment(getMethod, introspectedTable);
        getMethod = JavaElementGeneratorTools.generateMethodBody(
                getMethod,
                "return joinClause;"
        );
        FormatTools.addMethodWithBestPosition(topLevelClass, getMethod);

        // 添加 left Join
        addJoinMethod("innerJoin", "INNER JOIN", topLevelClass, introspectedTable);
        addJoinMethod("leftJoin", "LEFT JOIN", topLevelClass, introspectedTable);
        addJoinMethod("rightJoin", "RIGHT JOIN", topLevelClass, introspectedTable);
    }

    private void addJoinMethod(String joinMethodName, String joinSql, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        Method method1 = JavaElementGeneratorTools.generateMethod(
                joinMethodName,
                false, JavaVisibility.PUBLIC,
                topLevelClass.getType(),
                new Parameter(FullyQualifiedJavaType.getStringInstance(), "table"),
                new Parameter(FullyQualifiedJavaType.getStringInstance(), "on", true)
        );
        commentGenerator.addGeneralMethodComment(method1, introspectedTable);
        method1 = JavaElementGeneratorTools.generateMethodBody(
                method1,
                "StringBuffer sb = new StringBuffer();",
                "sb.append(\"" + joinSql + " \");",
                "sb.append(table);",
                "sb.append(\" ON \");",
                "for (int i = 0; i < on.length; i++) {",
                "sb.append(on[i]);",
                "if (i < on.length - 1) {",
                "sb.append(\" AND \");",
                "}",
                "}",
                "this.joinClause = sb.toString();",
                "return this;"
        );

        FormatTools.addMethodWithBestPosition(topLevelClass, method1);
    }


    /**
     * Example增强了setGroupByClause方法，新增groupBy(String orderByClause)方法直接返回example，增强链式调用，可以一路.下去了。
     *
     * @param topLevelClass
     * @param introspectedTable
     */
    private void addGroupByMethodToExample(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // 添加 groupByClause
        Field groupByClauseField = JavaElementGeneratorTools.generateField(
                "groupByClause",
                JavaVisibility.PROTECTED,
                new FullyQualifiedJavaType("java.lang.String"),
                null
        );
        commentGenerator.addFieldComment(groupByClauseField, introspectedTable);
        topLevelClass.addField(groupByClauseField);


        // 添加 getGroupByClause()
        Method getMethod = JavaElementGeneratorTools.generateMethod(
                "getGroupByClause",
                false, JavaVisibility.PUBLIC,
                new FullyQualifiedJavaType("java.lang.String")
        );
        commentGenerator.addGeneralMethodComment(getMethod, introspectedTable);
        getMethod = JavaElementGeneratorTools.generateMethodBody(
                getMethod,
                "return groupByClause;"
        );
        FormatTools.addMethodWithBestPosition(topLevelClass, getMethod);
        logger.debug("itfsw(Example增强插件):" + topLevelClass.getType().getShortName() + "增加方法getGroupByClause");

        // 添加groupBy
        Method method = JavaElementGeneratorTools.generateMethod(
                "groupBy",
                false, JavaVisibility.PUBLIC,
                topLevelClass.getType(),
                new Parameter(FullyQualifiedJavaType.getStringInstance(), "groupByClause")
        );
        commentGenerator.addGeneralMethodComment(method, introspectedTable);
        method = JavaElementGeneratorTools.generateMethodBody(
                method,
                "this.groupByClause = groupByClause;",
                "return this;"
        );
        FormatTools.addMethodWithBestPosition(topLevelClass, method);
        logger.debug("itfsw(Example增强插件):" + topLevelClass.getType().getShortName() + "增加方法groupBy");

        // 添加groupBy
        Method method1 = JavaElementGeneratorTools.generateMethod(
                "groupBy",
                false, JavaVisibility.PUBLIC,
                topLevelClass.getType(),
                new Parameter(FullyQualifiedJavaType.getStringInstance(), "groupByClause", true)
        );
        commentGenerator.addGeneralMethodComment(method1, introspectedTable);
        method1 = JavaElementGeneratorTools.generateMethodBody(
                method1,
                "StringBuffer sb = new StringBuffer();",
                "for (int i = 0; i < groupByClause.length; i++) {",
                "sb.append(groupByClause[i]);",
                "if (i < groupByClause.length - 1) {",
                "sb.append(\" , \");",
                "}",
                "}",
                "this.groupByClause = sb.toString();",
                "return this;"
        );

        FormatTools.addMethodWithBestPosition(topLevelClass, method1);
        logger.debug("itfsw(Example增强插件):" + topLevelClass.getType().getShortName() + "增加方法groupBy(String ... groupByClause)");
    }

    /**
     * Example增强了setOrderByClause方法，新增orderBy(String orderByClause)方法直接返回example，增强链式调用，可以一路.下去了。
     *
     * @param topLevelClass
     * @param introspectedTable
     */
    private void addOrderByMethodToExample(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {

        // 添加orderBy
        Method orderByMethod = JavaElementGeneratorTools.generateMethod(
                "orderBy",
                false, JavaVisibility.PUBLIC,
                topLevelClass.getType(),
                new Parameter(FullyQualifiedJavaType.getStringInstance(), "orderByClause")
        );
        commentGenerator.addGeneralMethodComment(orderByMethod, introspectedTable);
        orderByMethod = JavaElementGeneratorTools.generateMethodBody(
                orderByMethod,
                "this.setOrderByClause(orderByClause);",
                "return this;"
        );
        FormatTools.addMethodWithBestPosition(topLevelClass, orderByMethod);
        logger.debug("itfsw(Example增强插件):" + topLevelClass.getType().getShortName() + "增加方法orderBy");

        // 添加orderBy
        Method orderByMethod1 = JavaElementGeneratorTools.generateMethod(
                "orderBy",
                false, JavaVisibility.PUBLIC,
                topLevelClass.getType(),
                new Parameter(FullyQualifiedJavaType.getStringInstance(), "orderByClauses", true)
        );
        commentGenerator.addGeneralMethodComment(orderByMethod1, introspectedTable);
        orderByMethod1 = JavaElementGeneratorTools.generateMethodBody(
                orderByMethod1,
                "StringBuffer sb = new StringBuffer();",
                "for (int i = 0; i < orderByClauses.length; i++) {",
                "sb.append(orderByClauses[i]);",
                "if (i < orderByClauses.length - 1) {",
                "sb.append(\" , \");",
                "}",
                "}",
                "this.setOrderByClause(sb.toString());",
                "return this;"
        );

        FormatTools.addMethodWithBestPosition(topLevelClass, orderByMethod1);
        logger.debug("itfsw(Example增强插件):" + topLevelClass.getType().getShortName() + "增加方法orderBy(String ... orderByClauses)");
    }
}
