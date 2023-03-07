# soot-fact-generator
generate facts from bytecode (source is https://github.com/plast-lab/doop-mirror/tree/master/generators)

通过soot解析bytecode生成fact，类似于CodeQL创建database的过程，fact等价于codeql的database。

## build 
执行 `./gradlew build`进行编译，`soot-fact-generator.jar` 在 `build/libs` 目录

## usage

```
Usage: soot-fact-generator [options] file...
Options:
  --main <class>                        Specify the name of the main class.
  --ssa                                 Generate SSA facts, enabling flow-sensitive analysis.
  --full                                Generate facts by full transitive resolution.
  --allow-phantom                       Allow phantom classes.
  -d <directory>                        Specify where to generate output fact files.
  -i <archive>                          Find classes in <archive>.
  -i-dir <directory>                    Find classes/jar in <directory>.
  -l <archive>                          Find library classes in <archive>.
  -ld <archive>                         Find dependency classes in <archive>.
  -ld-dir <directory>                   Find dependency classes/jar in <directory>.
  -lsystem                              Find classes in default system classes.
  --facts-subset <subset>               Produce facts only for a subset of the given classes [APP, APP_N_DEPS, PLATFORM].
  --ignore-factgen-errors               Continue with the analysis even if fact generation fails.
  --legacy-android-processing           Enable legacy Android XML processing.
  --no-facts                            Don't generate facts (just empty files -- used for debugging).
  --ignore-wrong-staticness             Ignore "wrong static-ness" errors in Soot.
  --lowMem                              Consume less memory.
  --failOnMissingClasses <file>         Terminate if classes are missing (and record them to <file>).
  --also-resolve <class>                Force resolution of class that may not be found automatically.
  --debug                               Enable debug mode (verbose output).
  --log-dir <dir>                       Write logs in directory <dir>.
  --args-file <file>                    Read command-line arguments from <file> (one per line).
  --write-artifacts-map                 Write artifacts map.
Jimple/Shimple generation:
  --generate-jimple                     Generate Jimple/Shimple files in addition to other facts.
  --stdout                              Write Jimple/Shimple to stdout.
Android options:
  --android-jars <archive>              The main Android library JAR (for Android APK inputs). The same jar should be provided in the -l option.
  --decode-apk                          Decompress APK input in facts directory.
  --scan-native-code                    Scan native code found in JAR/APK inputs.
  --R-out-dir <directory>               Specify where to generate R code (when linking AAR inputs).

Supported input archive formats: AAR, APK, JAR, ZIP
```
常见的用法是
```
java -jar soot-fact-generator.jar -i input.jar  -l /usr/lib/jvm/java-8-oracle/jre/lib/rt.jar --generate-jimple --allow-phantom --full -d out
```
其中
- `-i` 指定待分析的jar包
- `-l` 指定依赖库
- `--generate-jimple` 表示生成中间语言jimple
- `--allow-phantom` 大概是允许解析依赖不存在的类
- `--full` 表示对所有class进行解析
- `-d` 指定输出目录

另外还额外增加了
- `-i-dir` 指定待分析的jar目录
- `-ld-dir` 指定待分析的依赖目录