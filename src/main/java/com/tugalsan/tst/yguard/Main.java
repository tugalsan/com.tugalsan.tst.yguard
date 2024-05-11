package com.tugalsan.tst.yguard;

import com.tugalsan.api.file.client.TGS_FileUtilsTur;
import com.tugalsan.api.file.html.server.TS_FileHtmlUtils;
import com.tugalsan.api.file.server.TS_DirectoryUtils;
import com.tugalsan.api.file.server.TS_PathUtils;
import com.tugalsan.api.file.txt.server.TS_FileTxtUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.stream.client.TGS_StreamUtils;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.url.client.TGS_Url;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    final static private TS_Log d = TS_Log.of(Main.class);

    private static void sniff(Path pathOutput, List<TGS_Url> sniffed, String name, TGS_Url urlA, TGS_Url urlB, TGS_Url urlABase, TGS_Url urlBBase) {
        //pre-check
        if (sniffed.stream().filter(u -> u.equals(urlA)).filter(u -> u.equals(urlB)).findAny().isPresent()) {
            return;
        }
        sniffed.add(urlA);
        sniffed.add(urlB);
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
        var lnk_a = TS_FileHtmlUtils.parseLinks(urlA, true, true);
        var lnk_b = TS_FileHtmlUtils.parseLinks(urlB, true, true);
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
        processLinks(pathOutput, sniffed, urlABase, urlBBase, lnk_a, lnk_b);
    }

    private static void processLinks(Path pathOutput, List<TGS_Url> sniffed, TGS_Url urlABase, TGS_Url urlBBase, List<TGS_Url> lnk_a, List<TGS_Url> lnk_b) {
        var packs_a = TGS_StreamUtils.toLst(lnk_a.stream().map(u -> new Link(u,
                TGS_FileUtilsTur.toSafe(u.toString().substring(urlABase.toString().length()))
        )));
        var packs_b = TGS_StreamUtils.toLst(lnk_b.stream().map(u -> new Link(u,
                TGS_FileUtilsTur.toSafe(u.toString().substring(urlBBase.toString().length()))
        )));
        List<Link> unprocessedA = new ArrayList();
        while (!packs_a.isEmpty()) {
            var packA = packs_a.getFirst();
            var packB = packs_b.stream().filter(p -> p.identifier.equals(packA.identifier)).findAny().orElse(null);
            if (packB == null) {
                unprocessedA.add(packA);
                packs_a.remove(packA);
                continue;
            }
            packs_a.remove(packA);
            packs_b.remove(packB);
            sniff(pathOutput, sniffed, packA.identifier, packA.url, packB.url, urlABase, urlBBase);
        }
    }

    record Link(TGS_Url url, String identifier) {

    }

    public static void main(String... s) {
        var urlA = TGS_Url.of("https://docs.oracle.com/javase/specs/jvms/se21/html/index.html");
        var urlB = TGS_Url.of("https://docs.oracle.com/javase/specs/jvms/se22/html/index.html");
        var pathOutput = TS_PathUtils.getPathCurrent_nio().resolve("output");
        TS_DirectoryUtils.deleteDirectoryIfExists(pathOutput);
        TS_DirectoryUtils.createDirectoriesIfNotExists(pathOutput);
        sniff(pathOutput, new ArrayList(), "_", urlA, urlB, urlA, urlB);
    }

}
