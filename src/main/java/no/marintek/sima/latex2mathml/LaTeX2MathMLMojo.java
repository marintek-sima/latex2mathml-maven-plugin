package no.marintek.sima.latex2mathml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import uk.ac.ed.ph.snuggletex.SerializationMethod;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;
import uk.ac.ed.ph.snuggletex.XMLStringOutputOptions;

@Mojo(name = "latex2mathml", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class LaTeX2MathMLMojo extends AbstractMojo {

	private static final String TARGET = "target";
	private static final String LATEX2MATHML = "latex2mathml";
	private static final String HELPGEN = "help-gen";

	private static final Pattern INLINE_EQUATION = Pattern.compile("\\$\\$?[^$]*\\$\\$?");

	@Parameter
	private List<File> tocs;

	public void execute() throws MojoExecutionException, MojoFailureException {
		for (File toc : tocs) {
			try {
				Path tocPath = toc.getCanonicalFile().toPath();
				copyTOC(tocPath);
				Path helpGenPath = tocPath.getParent().resolve(HELPGEN);
				Path copiedHelpGen = copyDirectory(helpGenPath);
				processDirectory(copiedHelpGen);
			} catch (IOException e) {
				throw new MojoExecutionException("Could not copy source directory " + toc, e);
			}
		}
	}

	private void copyTOC(Path tocPath) throws IOException {
		final Path targetPath = tocPath.getParent().resolve(TARGET).resolve(LATEX2MATHML);
		final File targetDirectory = targetPath.toFile();
		FileUtils.copyFileToDirectory(tocPath.toFile(), targetDirectory);
	}

	private Path copyDirectory(Path helpGenPath) throws IOException {
		final Path targetPath = helpGenPath.getParent().resolve(TARGET).resolve(LATEX2MATHML);
		final File targetDirectory = targetPath.toFile();
		FileUtils.copyDirectoryToDirectory(helpGenPath.toFile(), targetDirectory);
		return targetPath.resolve(helpGenPath.getFileName());
	}

	private void processDirectory(Path dir) throws IOException {
		Stream<Path> files = Files.walk(dir).filter(p -> p.toFile().isFile());
		Stream<Path> htmlFiles = files.filter(p -> {
			final String fileNameLowerCase = p.getFileName().toString().toLowerCase();
			return (fileNameLowerCase.endsWith(".html") || fileNameLowerCase.endsWith(".htm"));
		});
		for (Object path : htmlFiles.toArray()) {
			processHtmlFile((Path) path);
		}
	}

	private void processHtmlFile(Path htmlFile) throws IOException {
		getLog().info("Processing LaTeX -> MathML: " + htmlFile);
		String originalContent = FileUtils.readFileToString(htmlFile.toFile(), "UTF-8");
		String parsedContent = replaceLaTeXWithMathML(originalContent);
		FileUtils.writeStringToFile(htmlFile.toFile(), parsedContent, "UTF-8");
	}

	private String replaceLaTeXWithMathML(String text) throws IOException{
        StringBuffer sb = new StringBuffer();
        Matcher m = INLINE_EQUATION.matcher(text);
        while (m.find()){
			m.appendReplacement(sb, laTeX2MathMl(m.group()));
        }        
        m.appendTail(sb);
        return sb.toString();
	}

	private String laTeX2MathMl(String latex) throws IOException {
		// remove line breaks from within math expressions
		latex = latex.replace("<br></br>", "");
		latex = latex.replace("<br />", "");
		latex = latex.replace("<br/>", "");
		// back-convert html entities to utf-8
		String utf8latex = StringEscapeUtils.unescapeHtml4(latex);

		SnuggleEngine engine = new SnuggleEngine();
		SnuggleSession session = engine.createSession();
		SnuggleInput input = new SnuggleInput(utf8latex);
		session.parseInput(input);

		XMLStringOutputOptions options = new XMLStringOutputOptions();
		options.setSerializationMethod(SerializationMethod.XML);
		options.setIndenting(true);
		options.setEncoding("UTF-8");
		options.setAddingMathSourceAnnotations(false);
		options.setUsingNamedEntities(true);
		return session.buildXMLString(options);
	}
}
