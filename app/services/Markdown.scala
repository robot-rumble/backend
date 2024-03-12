package services

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.Sanitizers;

trait Markdown {
  def render(markdown: String): String;
}

class CommonMarkdown extends Markdown {
  val parser = Parser.builder().build();
  val renderer = HtmlRenderer.builder().build();
  val policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS).and(Sanitizers.BLOCKS);

  def render(markdown: String): String = {
    val document = parser.parse(markdown);
    val untrustedHTML = renderer.render(document);
    policy.sanitize(untrustedHTML);
  }
}
