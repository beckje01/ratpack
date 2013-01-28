package org.ratpackframework.templating.internal

import org.vertx.java.core.buffer.Buffer
import spock.lang.Specification

class TemplateParserTest extends Specification {

  private final TemplateParser parser = new TemplateParser()

  String parse(String source) {
    Buffer sourceBuffer = new Buffer(source)
    Buffer scriptBuffer = new Buffer(source.size())
    parser.parse(sourceBuffer, scriptBuffer)
    scriptBuffer.toString()
  }

  def "encoding"() {
    expect:
    parse("abc") == 'str("""abc""")'
    parse("aéc") == 'str("""aéc""")'
    parse("a\u1234c") == 'str("""a\u1234c""")'
  }

  def "gstrings"() {
    expect:
    parse("a\${'b'}c") == 'str("""a${\'b\'}c""")'
  }
}
