package com.tugalsan.tst.yguard;

import com.tugalsan.api.file.html.server.TS_FileHtmlUtils;
import com.tugalsan.api.file.server.TS_DirectoryUtils;
import com.tugalsan.api.file.server.TS_PathUtils;
import com.tugalsan.api.file.txt.server.TS_FileTxtUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.url.client.TGS_Url;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    final static private TS_Log d = TS_Log.of(Main.class);

    private static Path pathOutput() {
        var pathOutput = TS_PathUtils.getPathCurrent_nio().resolve("output");
        TS_DirectoryUtils.createDirectoriesIfNotExists(pathOutput);
        d.cr("pathOutput", pathOutput);
        return pathOutput;
    }

    private static void process(Path pathOutput, List<TGS_Url> processed, String name, TGS_Url urlA, TGS_Url urlB) {
        //pre-check
        if (processed.stream().filter(u -> u.equals(urlA)).filter(u -> u.equals(urlB)).findAny().isPresent()) {
            return;
        }
        processed.add(urlA);
        processed.add(urlB);
        //write txt
        TS_FileTxtUtils.toFile(
                TS_FileHtmlUtils.toText(urlA),
                pathOutput.resolve(name + "_txt_a.txt"),
                false
        );
        TS_FileTxtUtils.toFile(
                TS_FileHtmlUtils.toText(urlB),
                pathOutput.resolve(name + "_txt_b.txt"),
                false
        );
        //parse links
        var lnk_a = TS_FileHtmlUtils.parseLinks(urlA);
        var lnk_b = TS_FileHtmlUtils.parseLinks(urlB);
        //write links
        TS_FileTxtUtils.toFile(
                TGS_StringUtils.toString(lnk_a, "\n"),
                pathOutput.resolve(name + "_lnk_a.txt"),
                false
        );
        TS_FileTxtUtils.toFile(
                TGS_StringUtils.toString(lnk_b, "\n"),
                pathOutput.resolve(name + "_lnk_b.txt"),
                false
        );
        //process links 
        //TODO
    }

    private static void main(String... s) {
        process(pathOutput(), new ArrayList(), "home",
                TGS_Url.of("https://docs.oracle.com/javase/specs/jvms/se21/html/index.html"),
                TGS_Url.of("https://docs.oracle.com/javase/specs/jvms/se22/html/index.html")
        );
    }

}
