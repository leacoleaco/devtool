/*
 * Copyright (c) 2018.
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

package pro.leaco.mybatis.generator.plugins.utils.hook;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.List;

/**
 * ---------------------------------------------------------------------------
 * <p>
 * ---------------------------------------------------------------------------
 *
 * @author: hewei
 * @time:2018/4/28 17:29
 * ---------------------------------------------------------------------------
 */
public interface IModelBuilderPluginHook {
    /**
     * Model builder class 生成
     *
     * @param topLevelClass
     * @param builderClass
     * @param columns
     * @param introspectedTable
     * @return
     */
    boolean modelBuilderClassGenerated(TopLevelClass topLevelClass, InnerClass builderClass, List<IntrospectedColumn> columns, IntrospectedTable introspectedTable);

    /**
     * Model builder set 方法生成
     *
     * @param method
     * @param topLevelClass
     * @param builderClass
     * @param introspectedColumn
     * @param introspectedTable
     * @return
     */
    boolean modelBuilderSetterMethodGenerated(Method method, TopLevelClass topLevelClass, InnerClass builderClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable);
}
