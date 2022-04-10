package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h2>API参数或返回值实体字段的样例值，用于提高API文档的质量</h2>
 *
 * <p>在声明API声明中，为了描述清楚整个API的输入输出，仅描述其参数和返回值类型是不够的，
 * 有时候给出一个具体的例子可能更有利于理解。</p>
 * <br>
 *
 * <p>在另外一些情况下，一些自动化测试程序或辅助测试功能（例如mock）需要根据某个API的文档构造出这个API的一个合法参数和返回值。
 * 在这时，声明API时提供这些信息将会给这些自动化程序带来便利。</p>
 * <br>
 *
 * <h3>API参数或返回值的样例值按照如下要求声明：</h3>
 * <ol>
 *     <li>API的参数和返回值均可以声明样例值</li>
 *     <li>如果参数类型为实体类型，则在其实体类型的各个字段上使用 @ExampleValue 声明样例值；<br>
 *         否则使用 @ApiParameter.exampleValue 对参数样例值进行声明</li>
 *     <li>如果返回值类型为实体类型，则在其实体类型的各个字段上使用 @ExampleValue 声明其各字段的样例值；<br>
 *         否则使用 @HttpApi.exampleValue 直接对API返回值的样例值进行声明</li>
 *     <li>如果实体字段的类型也是实体类型，使用 @ExampleValue 对该实体的各个字段进行声明；对于嵌套实体类型均使用此方式声明</li>
 * </ol>
 *
 * <h3>API参数或返回值的最终样例值通过如下方式确定：</h3>
 * <ol>
 *     <li>检查 @HttpApi.exampleValue 或 @ApiParameter.exampleValue，
 *     如果该值已声明（非空），则将其JSON反序列化为参数或返回值类型，得到最终样例值对象</li>
 *     <li>否则，如果参数或返回值类型为实体类型，则构造该实体的对象，实体的每个声明的字段按照如下方式确定其样例值：</li>
 *     <li>检查字段是否声明了 @ExampleValue，如果有，则将其JSON反序列化为字段的声明类型，将得到的结果作为此字段的最终样例值</li>
 *     <li>否则，如果字段是实体类型，则构造出新的实体，按照上述方法确定样例值。
 *     为防止出现递归，求值过程会在嵌套路径中第一次遇到重复类型时退出，将字段样例值留空（null）</li>
 *     <li>否则，如果字段是集合类型，并且元素类型为实体类型，则构造出一个仅包含一个元素的集合，该元素为其元素类型的对象</li>
 *     <li>无法确定字段样例值，根据字段类型产生随机数</li>
 * </ol>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExampleValue {
    /**
     * 该字段的样例值，值的格式应与所修饰字段的类型的JSON序列化后格式一致。<br>
     * 即所提供值应能够被反序列化为字段的声明类型
     * @return 字段的样例值
     */
    String value();
}
