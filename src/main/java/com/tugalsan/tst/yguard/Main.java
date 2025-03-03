package com.tugalsan.tst.yguard;

import com.tugalsan.api.file.client.TGS_FileUtilsTur;
import com.tugalsan.api.file.html.server.TS_FileHtmlUtils;
import com.tugalsan.api.file.server.TS_DirectoryUtils;
import com.tugalsan.api.file.server.TS_PathUtils;
import com.tugalsan.api.file.txt.server.TS_FileTxtUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.stream.client.TGS_StreamUtils;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import com.tugalsan.api.url.client.TGS_Url;
import com.tugalsan.api.url.client.TGS_UrlUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    final static private TS_Log d = TS_Log.of(Main.class);

    private static void sniff(Path pathOutput, List<TGS_Url> sniffed, String name, TGS_Url urlA, TGS_Url urlB) {
        d.cr("sniff", "init", "--------------------------");
        d.cr("sniff", "pathOutput", pathOutput);
        d.cr("sniff", "name", name);
        d.cr("sniff", "urlA", urlA);
        d.cr("sniff", "urlB", urlB);
        sniffed.forEach(u -> {
            d.cr("sniff", "sniffed.u", u);
        });

        //pre-check
        if (sniffed.stream().filter(u -> u.equals(urlA)).anyMatch(u -> u.equals(urlB))) {
            d.ce("sniff", "notfound.u", urlA);
            return;
        }
        sniffed.add(urlA);
        sniffed.add(urlB);
        var pathOutputName = pathOutput.resolve(name);
        d.cr("sniff", "pathOutputName", pathOutputName);
        //get text
        var txt_a = TGS_UnSafe.call(() -> TS_FileHtmlUtils.toText(urlA), e -> null);
        var txt_b = TGS_UnSafe.call(() -> TS_FileHtmlUtils.toText(urlB), e -> null);
        if (txt_a == null || txt_b == null) {
            var path_skipped_a = pathOutputName.resolve("_skipped_a.txt");
            TS_FileTxtUtils.toFile(urlA.url, path_skipped_a, false);
            var path_skipped_b = pathOutputName.resolve("_skipped_b.txt");
            TS_FileTxtUtils.toFile(urlB.url, path_skipped_b, false);
            return;
        }
        //write txt
        var pathtxt_a = pathOutputName.resolve(name + "_txt_a.txt");
        d.cr("sniff", "pathtxt_a", pathtxt_a);
        TS_FileTxtUtils.toFile(txt_a, pathtxt_a, false);
        var pathtxt_b = pathOutputName.resolve(name + "_txt_b.txt");
        d.cr("sniff", "pathtxt_b", pathtxt_b);
        TS_FileTxtUtils.toFile(txt_b, pathtxt_b, false);
        //parse links
        var links_a = TS_FileHtmlUtils.parseLinks(urlA, true, true);
        var links_b = TS_FileHtmlUtils.parseLinks(urlB, true, true);
        //write links
        var pathlnk_a = pathOutputName.resolve(name + "_lnk_a.txt");
        d.cr("sniff", "pathlnk_a", pathlnk_a);
        TS_FileTxtUtils.toFile(TGS_StringUtils.toString(links_a, "\n"), pathlnk_a, false);
        var pathlnk_b = pathOutputName.resolve(name + "_lnk_b.txt");
        d.cr("sniff", "pathlnk_b", pathlnk_b);
        TS_FileTxtUtils.toFile(TGS_StringUtils.toString(links_b, "\n"), pathlnk_b, false);
        //process links 
        processLinks(pathOutput, sniffed, urlA, urlB, links_a, links_b);
    }

    private static void processLinks(Path pathOutput, List<TGS_Url> sniffed, TGS_Url urlABase, TGS_Url urlBBase, List<TGS_Url> links_a, List<TGS_Url> links_b) {
        d.cr("processLinks", "init", "--------------------------");
        d.cr("processLinks", "pathOutput", pathOutput);
        d.cr("processLinks", "urlABase", urlABase);
        d.cr("processLinks", "urlBBase", urlBBase);
        links_a.forEach(u -> {
            d.cr("sniff", "links_a.u", u);
        });
        links_b.forEach(u -> {
            d.cr("sniff", "links_b.u", u);
        });
        sniffed.forEach(u -> {
            d.cr("sniff", "sniffed.u", u);
        });
        var packs_a = TGS_StreamUtils.toLst(links_a.stream().map(u -> new Link(u,
                urlABase.url.length() > u.url.length() ? "directory"
                : TGS_FileUtilsTur.toSafe(u.toString().substring(urlABase.toString().length()))
        )));
        var packs_b = TGS_StreamUtils.toLst(links_b.stream().map(u -> new Link(u,
                urlBBase.url.length() > u.url.length() ? "directory"
                : TGS_FileUtilsTur.toSafe(u.toString().substring(urlBBase.toString().length()))
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
            sniff(pathOutput, sniffed, packA.identifier, packA.url, packB.url);
        }
        //write unprocessed links
        if (!unprocessedA.isEmpty()) {
            var path_unprocessed_links_a = pathOutput.resolve("_unprocessed_links_a.txt");
            d.cr("processLinks", "path_unprocessed_links_a", path_unprocessed_links_a);
            TS_FileTxtUtils.toFile(TGS_StringUtils.toString(unprocessedA, "\n"), path_unprocessed_links_a, true);
        }
        if (!packs_b.isEmpty()) {
            var path_unprocessed_links_b = pathOutput.resolve("_unprocessed_links_b.txt");
            d.cr("processLinks", "path_unprocessed_links_a", path_unprocessed_links_b);
            TS_FileTxtUtils.toFile(TGS_StringUtils.toString(packs_b, "\n"), path_unprocessed_links_b, true);
        }
    }

    record Link(TGS_Url url, String identifier) {

        @Override
        public String toString() {
            return url.toString();
        }
    }

    public static void main(String... s) {
        var urlA = TGS_Url.of("https://docs.oracle.com/javase/specs/jvms/se21/html/");
        var urlB = TGS_Url.of("https://docs.oracle.com/javase/specs/jvms/se22/html/");
        var pathOutput = TS_PathUtils.getPathCurrent_nio().resolve("output");
        TS_DirectoryUtils.deleteDirectoryIfExists(pathOutput);
        TS_DirectoryUtils.createDirectoriesIfNotExists(pathOutput);
        sniff(pathOutput, new ArrayList(), TGS_FileUtilsTur.toSafe(TGS_UrlUtils.getFileNameFull(urlA)), urlA, urlB);
    }

}
