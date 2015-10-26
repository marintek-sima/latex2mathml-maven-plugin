# LaTeX2MathML
A maven mojo to convert LaTeX equations to MathML using SnuggleTeX

Original html files are assumed to be in `./help-gen`. Directories with converted files are copied to `./target/latex2mathml`. 

```
<plugin>
	<groupId>no.marintek.sima</groupId>
	<artifactId>latex2mathml-maven-plugin</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<executions>
		<execution>
			<id>latex2mathml</id>
			<phase>process-resources</phase>
			<goals>
				<goal>latex2mathml</goal>
			</goals>
			<configuration>
				<tocs>
					<toc>${project.basedir}/toc.xml</toc>
					<toc>${project.basedir}/../someotherproject/toc.xml</toc>
				</tocs>
			</configuration>
		</execution>
	</executions>
</plugin>

```