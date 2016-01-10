package com.vigi;

import com.haulmont.yarg.formatters.factory.DefaultFormatterFactory;
import com.haulmont.yarg.loaders.factory.DefaultLoaderFactory;
import com.haulmont.yarg.loaders.impl.GroovyDataLoader;
import com.haulmont.yarg.reporting.ReportOutputDocument;
import com.haulmont.yarg.reporting.Reporting;
import com.haulmont.yarg.reporting.RunParams;
import com.haulmont.yarg.structure.Report;
import com.haulmont.yarg.structure.ReportBand;
import com.haulmont.yarg.structure.ReportOutputType;
import com.haulmont.yarg.structure.impl.BandBuilder;
import com.haulmont.yarg.structure.impl.ReportBuilder;
import com.haulmont.yarg.structure.impl.ReportFieldFormatImpl;
import com.haulmont.yarg.structure.impl.ReportTemplateBuilder;
import com.haulmont.yarg.util.groovy.DefaultScriptingImpl;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by vigi on 1/9/2016.
 */
@Controller
class ReportController {

    @RequestMapping(value = "/reports/{output}", method = RequestMethod.GET)
    void downloadFile(@PathVariable String output, HttpServletResponse response) throws IOException {
        ReportBuilder reportBuilder = new ReportBuilder();
        ReportTemplateBuilder reportTemplateBuilder = new ReportTemplateBuilder()
                .documentContent(new ClassPathResource("invoice.docx").getInputStream())
                .documentPath("invoice.docx")
                .documentName("invoice.docx")
                .outputType(ReportOutputType.getOutputTypeById(output));
        reportBuilder.template(reportTemplateBuilder.build());
        BandBuilder bandBuilder = new BandBuilder();
        ReportBand main = bandBuilder.name("Main").query("Main", "return [\n" +
                "                              [\n" +
                "                               'invoiceNumber':99987,\n" +
                "                               'client' : 'Google Inc.',\n" +
                "                               'date' : new Date(),\n" +
                "                               'addLine1': '1600 Amphitheatre Pkwy',\n" +
                "                               'addLine2': 'Mountain View, USA',\n" +
                "                               'addLine3':'CA 94043',\n" +
                "                               'signature':'<html><body><b><span style=\"color:red\">Mr. Yarg</span></b></body></html>'\n" +
                "                            ]]", "groovy").build();


        bandBuilder = new BandBuilder();
        ReportBand items = bandBuilder.name("Items").query("Items", "return [\n" +
                "                                ['name':'Java Concurrency in practice', 'price' : 15000],\n" +
                "                                ['name':'Clear code', 'price' : 13000],\n" +
                "                                ['name':'Scala in action', 'price' : 12000]\n" +
                "                            ]", "groovy").build();

        reportBuilder.band(main);
        reportBuilder.band(items);
        reportBuilder.format(new ReportFieldFormatImpl("Main.signature", "${html}"));

        Report report = reportBuilder.build();

        Reporting reporting = new Reporting();
        reporting.setFormatterFactory(new DefaultFormatterFactory());
        reporting.setLoaderFactory(
                new DefaultLoaderFactory().setGroovyDataLoader(new GroovyDataLoader(new DefaultScriptingImpl())));

        String contentType = null;
        if ("pdf".equalsIgnoreCase(output)) {
            contentType = "application/pdf";
        } else if ("docx".equalsIgnoreCase(output)) {
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if ("html".equalsIgnoreCase(output)) {
            contentType = "text/html";
        }
        response.setContentType(contentType);

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                String.format("attachment; filename=\"%s\"", "invoice." + output));
        ReportOutputDocument reportOutputDocument = reporting.runReport(new RunParams(report), response.getOutputStream());
        System.out.println(reportOutputDocument.getDocumentName() + " - " + contentType);
    }
}
