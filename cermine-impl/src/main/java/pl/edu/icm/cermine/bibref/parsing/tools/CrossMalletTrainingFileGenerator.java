/**
 * This file is part of CERMINE project.
 * Copyright (c) 2011-2013 ICM-UW
 *
 * CERMINE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CERMINE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with CERMINE. If not, see <http://www.gnu.org/licenses/>.
 */

package pl.edu.icm.cermine.bibref.parsing.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;
import pl.edu.icm.cermine.bibref.model.BibEntry;
import pl.edu.icm.cermine.bibref.parsing.model.Citation;
import pl.edu.icm.cermine.bibref.parsing.model.CitationToken;
import pl.edu.icm.cermine.bibref.transformers.BibEntryToNLMElementConverter;
import pl.edu.icm.cermine.exception.TransformationException;

/**
 *
 * @author Dominika Tkaczyk
 */
public final class CrossMalletTrainingFileGenerator {

    private static String nlmFile = "/home/domin/phd-metadata-extraction/results/citations/dataset/citations.nxml";
    private static String outFile = "/home/domin/phd-metadata-extraction/results/citations/training/training";
    private static String outValidFile = "/home/domin/phd-metadata-extraction/results/citations/training/validation";
   

    public static void main(String[] args) throws JDOMException, IOException, TransformationException {

        File file = new File(nlmFile);
        try {
            List<Citation> traincitations = new ArrayList<Citation>();
            List<Citation> validcitations = new ArrayList<Citation>();

            InputStream is = null;
            List<Citation> citations;
            try {
                is = new FileInputStream(file);
                InputSource source = new InputSource(is);
                citations = NlmCitationExtractor.extractCitations(source);
            } finally {
                if (is != null) {
                    is.close();
                }
            }

            Collections.shuffle(citations, new Random(5394));
            traincitations.addAll(citations.subList(0, 2000));
            validcitations.addAll(citations.subList(2000, 4000));

            File validFile = new File(outValidFile);
            for (Citation citation : citations) {
                for (CitationToken ct : citation.getTokens()) {
                    if (ct.getText().matches("^[a-zA-Z]+$")) {
                        FileUtils.writeStringToFile(validFile, ct.getText().toLowerCase(), true);
                        FileUtils.writeStringToFile(validFile, "\n", true);
                    }
                }
            }
            
            List<Citation>[] folds = new List[5];
            for (int idx = 0; idx < traincitations.size(); idx++) {
                if (idx < 5) {
                    folds[idx] = new ArrayList<Citation>();
                }
                folds[idx % 5].add(traincitations.get(idx));
            }

            Writer[] trainWriters = new Writer[5];
            Writer[] testWriters = new Writer[5];
            Writer[] nlmWriters = new Writer[5];
            for (int idx = 0; idx < 5; idx++) {
                trainWriters[idx] = new FileWriter(outFile+".train."+idx);
                testWriters[idx] = new FileWriter(outFile+".test."+idx);
                nlmWriters[idx] = new FileWriter(outFile+".nlm."+idx);
            }
            
            for (int idx = 0; idx < folds.length; idx++) {
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                BibEntryToNLMElementConverter conv = new BibEntryToNLMElementConverter();
                Element el = new Element("refs");
                int k = 1;
                
                for (Citation be : folds[idx]) {
                    BibEntry bb = CitationUtils.citationToBibref(be);
                    Element e = conv.convert(bb);
                    e.setAttribute("id", String.valueOf(k));
                    el.addContent(e);
                    k++;
                }
                
                nlmWriters[idx].write(outputter.outputString(el));
            }
                        
            for (int idx = 0; idx < traincitations.size(); idx++) {
                List<String> tokens = CitationUtils.citationToMalletInputFormat(traincitations.get(idx));
                String ma = StringUtils.join(tokens, "\n")+"\n\n";
                for (int fIdx = 0; fIdx < 5; fIdx++) {
                    if (fIdx == idx % 5) {
                        testWriters[fIdx].write(ma);
                        
                    } else {
                        trainWriters[fIdx].write(ma);
                    }
                }
            }
            
            for (int idx = 0; idx < 5; idx++) {
                trainWriters[idx].flush();
                testWriters[idx].flush();
                nlmWriters[idx].flush();
            }
        } finally {

        }
    }

    private CrossMalletTrainingFileGenerator() {
    }
}
