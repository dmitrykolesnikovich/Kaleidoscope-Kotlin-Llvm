import kotlinx.cinterop.toKString
import llvm.LLVMPrintModuleToString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Alex Plate
 */

@ExperimentalUnsignedTypes
class CodeGeneratorTest {
    @Test
    internal fun `simple expression`() {
        val data = LlvmData(false)
        CodeGenerator(data).generate("1 + 2")
        val expected = """
           define i64 @__anon_expr() {
           entry:
             ret i64 3
           }
           
        """.trimIndent()
        assertEquals(expected, getString(data))
    }

    @Test
    internal fun `expression with parentheses`() {
        val data = LlvmData(false)
        CodeGenerator(data).generate("4 * (2 + 3)")
        val expected = """
           define i64 @__anon_expr() {
           entry:
             ret i64 20
           }
           
        """.trimIndent()
        assertEquals(expected, getString(data))
    }

    @Test
    internal fun function() {
        val data = LlvmData(false)
        CodeGenerator(data).generate("def myFunction(x) x + 1")
        val expected = """
           define i64 @myFunction(i64 %x) {
           entry:
             %addtmp = add i64 %x, 1
             ret i64 %addtmp
           }
           
        """.trimIndent()
        assertEquals(expected, getString(data))
    }

    @Test
    internal fun `function without optimizations`() {
        val data = LlvmData(false)
        CodeGenerator(data).generate("def test(x) (1+2+x)*(x+(1+2))")
        val expected = """
           define i64 @test(i64 %x) {
           entry:
             %addtmp = add i64 3, %x
             %addtmp1 = add i64 %x, 3
             %multmp = mul i64 %addtmp, %addtmp1
             ret i64 %multmp
           }
           
        """.trimIndent()
        assertEquals(expected, getString(data))
    }

    @Test
    internal fun `function with optimizations`() {
        val data = LlvmData(true)
        CodeGenerator(data).generate("def test(x) (1+2+x)*(x+(1+2))")
        val expected = """
           define i64 @test(i64 %x) {
           entry:
             %addtmp = add i64 %x, 3
             %multmp = mul i64 %addtmp, %addtmp
             ret i64 %multmp
           }
           
        """.trimIndent()
        assertEquals(expected, getString(data))
    }

    @Test
    internal fun `if condition`() {
        val data = LlvmData(true)
        CodeGenerator(data).generate(
            """
                extern foo();
                extern bar();
                def baz(x) if x then foo() else bar();
                """.trimIndent()
        )
        val expected = """
           declare i64 @foo()

           declare i64 @bar()

           define i64 @baz(i64 %x) {
           entry:
             %ifcond = icmp eq i64 %x, 0
             br i1 %ifcond, label %else, label %then

           then:                                             ; preds = %entry
             %calltmp = call i64 @foo()
             br label %ifcont

           else:                                             ; preds = %entry
             %calltmp1 = call i64 @bar()
             br label %ifcont

           ifcont:                                           ; preds = %else, %then
             %iftmp = phi i64 [ %calltmp, %then ], [ %calltmp1, %else ]
             ret i64 %iftmp
           }

        """.trimIndent()
        assertEquals(expected, getString(data))
    }

    @Test
    internal fun `for loop`() {
        val data = LlvmData(false)
        CodeGenerator(data).generate(
            """
            extern putchard(char);
            def printstar(n)
              for i = 1, i < n, 1 in
                putchard(42);
            """.trimIndent()
        )
        val expected = """
           declare i64 @putchard(i64)

           define i64 @printstar(i64 %n) {
           entry:
             br label %loop

           loop:                                             ; preds = %loop, %entry
             %i = phi i64 [ 1, %entry ], [ %nextvar, %loop ]
             %calltmp = call i64 @putchard(i64 42)
             %nextvar = add i64 %i, 1
             %ltcode = icmp slt i64 %i, %n
             %loopcond = icmp ne i1 %ltcode, i64 0
             br i1 %loopcond, label %loop, label %afterloop

           afterloop:                                        ; preds = %loop
             ret i64 0
           }

        """.trimIndent()
        assertEquals(expected, getString(data))
    }

    private fun getString(data: LlvmData): String? {
        val str = LLVMPrintModuleToString(data.module)?.toKString() ?: ""
        return str.substring(str.indexOf("\n\n") + 2)
    }

}
