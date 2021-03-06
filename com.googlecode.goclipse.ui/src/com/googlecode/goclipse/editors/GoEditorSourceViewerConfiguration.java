package com.googlecode.goclipse.editors;

import static melnorme.utilbox.core.CoreUtil.array;
import melnorme.lang.ide.ui.LangUIPlugin;
import melnorme.lang.ide.ui.editor.BestMatchHover;
import melnorme.util.swt.jface.ColorManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import com.googlecode.goclipse.Activator;
import com.googlecode.goclipse.ui.GoUIPreferenceConstants;
import com.googlecode.goclipse.ui.editor.text.GoAutoEditStrategy;
import com.googlecode.goclipse.ui.text.GoPartitions;
import com.googlecode.goclipse.ui.util.SingleTokenScanner;
import com.googlecode.goclipse.utils.IContentAssistProcessorExt;

/**
 * @author steel
 */
public class GoEditorSourceViewerConfiguration extends TextSourceViewerConfiguration {
	
	private DoubleClickStrategy	doubleClickStrategy;
	private GoScanner	        keywordScanner;
	private GoEditor	        editor;
	private MonoReconciler	    reconciler;

	public GoEditorSourceViewerConfiguration(GoEditor editor, IPreferenceStore preferenceStore) {
		super(preferenceStore);

		this.editor = editor;
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return GoPartitions.PARTITION_TYPES;
	}
	
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new DoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected GoScanner getKeywordScanner() {
		if (keywordScanner == null) {
			keywordScanner = new GoScanner();
		}
		return keywordScanner;
	}
	
	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return GoPartitions.PARTITIONING_ID;
	}
	
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler= new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getKeywordScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		boolean useHighlighting = fPreferenceStore.getBoolean(GoUIPreferenceConstants.FIELD_USE_HIGHLIGHTING);
		
		if (useHighlighting) {
			
			setupSingleTokenDamagerRepairer(reconciler, PartitionScanner.COMMENT, 
				GoUIPreferenceConstants.FIELD_SYNTAX_COMMENT_COLOR, 
				GoUIPreferenceConstants.FIELD_SYNTAX_COMMENT_STYLE);

			setupSingleTokenDamagerRepairer(reconciler, PartitionScanner.STRING, 
				GoUIPreferenceConstants.FIELD_SYNTAX_STRING_COLOR, 
				GoUIPreferenceConstants.FIELD_SYNTAX_STRING_STYLE);
			
			setupSingleTokenDamagerRepairer(reconciler, PartitionScanner.MULTILINE_STRING, 
				GoUIPreferenceConstants.FIELD_SYNTAX_MULTILINE_STRING_COLOR, 
				GoUIPreferenceConstants.FIELD_SYNTAX_MULTILINE_STRING_STYLE);
			
		}
		return reconciler;
	}
	
	protected void setupSingleTokenDamagerRepairer(PresentationReconciler reconciler, String contentType, 
			String colorKey, String styleKey) {
		Color commentColor = ColorManager.INSTANCE.getColor(
			PreferenceConverter.getColor(fPreferenceStore, colorKey));
		int commentStyle = fPreferenceStore.getInt(styleKey);
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new SingleTokenScanner(commentColor, commentStyle));
		reconciler.setDamager(dr, contentType);
		reconciler.setRepairer(dr, contentType);
	}
	
	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sv) {
		final ContentAssistant ca = new ContentAssistant();
		ca.enableAutoActivation(true);
		ca.setAutoActivationDelay(100);

		ca.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		ca.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		ca.setContextInformationPopupBackground(ColorManager.INSTANCE.getColor(new RGB(150, 150, 0)));
		ca.setInformationControlCreator(getInformationControlCreator(sv));

		IContentAssistProcessor cap = getCompletionProcessor();
		ca.setContentAssistProcessor(cap, IDocument.DEFAULT_CONTENT_TYPE);
		// ca.setInformationControlCreator(getInformationControlCreator(sv));
		return ca;
	}

	/**
	 * @return
	 */
	private IContentAssistProcessor getCompletionProcessor() {
		
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
		        Activator.CONTENT_ASSIST_EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				final Object extension = e.createExecutableExtension("class");

				if (extension instanceof IContentAssistProcessorExt) {
					((IContentAssistProcessorExt) extension).setEditorContext(editor);
				}

				if (extension instanceof IContentAssistProcessor) {
					return (IContentAssistProcessor) extension;
				}
			}
		} catch (CoreException ex) {
			// do nothing
		}
		
		return new CompletionProcessor(editor);
	}
	
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
		if(contentType.equals(IDocument.DEFAULT_CONTENT_TYPE)) {
			return new BestMatchHover(editor, stateMask);
		}
		return new TextHover();
	}
	
	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new DefaultAnnotationHover();
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		
		if (sourceViewer == null) {
			return null;
		}

		return new IHyperlinkDetector[] { new URLHyperlinkDetector(), new GoHyperlinkDetector() };
	}

	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		
		if (reconciler == null && sourceViewer != null) {
			reconciler = new MonoReconciler(new GoEditorReconcilingStrategy(editor), false);
			reconciler.setDelay(500);
		}

		return reconciler;
	}

	@Override
	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "//", "" };
	}

	@Override
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "\t", "" };
	}

	@Override
	public int getTabWidth(ISourceViewer sourceViewer) {
		
		if (fPreferenceStore == null) {
			return super.getTabWidth(sourceViewer);
		}
		
		return fPreferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
	}
	
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		if(IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
			return array(new GoAutoEditStrategy(LangUIPlugin.getPrefStore(), contentType, sourceViewer));
		} else {
			return super.getAutoEditStrategies(sourceViewer, contentType);
		}
	}
	
}
