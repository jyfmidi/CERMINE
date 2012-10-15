package pl.edu.icm.cermine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.structure.*;
import pl.edu.icm.cermine.structure.model.BxDocument;
import pl.edu.icm.cermine.structure.tools.BxModelUtils;


/**
 *
 * @author Dominika Tkaczyk
 */
public class PdfBxStructureExtractor implements DocumentStructureExtractor {
    
    private CharacterExtractor glyphExtractor;
    
    private PageSegmenter pageSegmenter;
    
    private ReadingOrderResolver roResolver;
    
    private ZoneClassifier initialClassifier;


    public PdfBxStructureExtractor() throws AnalysisException {
        glyphExtractor = new ITextCharacterExtractor();
        pageSegmenter = new DocstrumPageSegmenter();
        roResolver = new HierarchicalReadingOrderResolver();
        
        InputStreamReader modelISRI = new InputStreamReader(PdfBxStructureExtractor.class
				.getResourceAsStream("/pl/edu/icm/cermine/structure/svm_initial_classifier"));
        BufferedReader modelFileI = new BufferedReader(modelISRI);
        InputStreamReader rangeISRI = new InputStreamReader(PdfBxStructureExtractor.class
		        .getResourceAsStream("/pl/edu/icm/cermine/structure/svm_initial_classifier.range"));
        BufferedReader rangeFileI = new BufferedReader(rangeISRI);
        initialClassifier = new SVMInitialZoneClassifier(modelFileI, rangeFileI);
    }
    
    public PdfBxStructureExtractor(InputStream model, InputStream range) throws AnalysisException {
        glyphExtractor = new ITextCharacterExtractor();
        pageSegmenter = new DocstrumPageSegmenter();
        roResolver = new HierarchicalReadingOrderResolver();
        
        InputStreamReader modelISRI = new InputStreamReader(model);
        BufferedReader modelFileI = new BufferedReader(modelISRI);
        InputStreamReader rangeISRI = new InputStreamReader(range);
        BufferedReader rangeFileI = new BufferedReader(rangeISRI);
        initialClassifier = new SVMInitialZoneClassifier(modelFileI, rangeFileI);
    }

    public PdfBxStructureExtractor(CharacterExtractor glyphExtractor, PageSegmenter pageSegmenter, 
            ReadingOrderResolver roResolver, ZoneClassifier initialClassifier) {
        this.glyphExtractor = glyphExtractor;
        this.pageSegmenter = pageSegmenter;
        this.roResolver = roResolver;
        this.initialClassifier = initialClassifier;
    }
    
        
    @Override
    public BxDocument extractStructure(InputStream stream) throws AnalysisException {
        BxDocument doc = glyphExtractor.extractCharacters(stream);
        doc = pageSegmenter.segmentPages(doc);
        BxModelUtils.setParents(doc);
        doc = roResolver.resolve(doc);
        return initialClassifier.classifyZones(doc);
    }

    public void setGlyphExtractor(CharacterExtractor glyphExtractor) {
        this.glyphExtractor = glyphExtractor;
    }

    public void setInitialClassifier(ZoneClassifier initialClassifier) {
        this.initialClassifier = initialClassifier;
    }

    public void setPageSegmenter(PageSegmenter pageSegmenter) {
        this.pageSegmenter = pageSegmenter;
    }

    public void setRoResolver(ReadingOrderResolver roResolver) {
        this.roResolver = roResolver;
    }
   
}