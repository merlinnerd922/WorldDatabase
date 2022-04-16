package edu.stanford.nlp.pipeline;

/**
 * Wrapper around a CoreMap representing a sentence.  Adds some helpful methods.
 *
 */

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.semgraph.*;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.tregex.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import kotlinx.serialization.Serializable;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;
@kotlinx.serialization.Serializable
public class CoreSentence {

  private final CoreDocument document;
  private final CoreMap sentenceCoreMap;
  private List<CoreEntityMention> entityMentions;

  /** common patterns to search for constituency parses **/
  private static final TregexPattern nounPhrasePattern = TregexPattern.compile("NP");
  private static final TregexPattern verbPhrasePattern = TregexPattern.compile("VP");

  /** cache to hold general patterns **/
  private static final ConcurrentHashMap<String, TregexPattern> patternCache = new ConcurrentHashMap<>();
  private static final Function<String,TregexPattern> compilePattern = TregexPattern::compile;
  private static final Function<Tree, String> treeToSpanString = Tree::spanString;

  public CoreSentence(CoreDocument myDocument, CoreMap coreMapSentence) {
    this.document = myDocument;
    this.sentenceCoreMap = coreMapSentence;
  }

  /** create list of CoreEntityMention's based on the CoreMap's entity mentions **/
  public void wrapEntityMentions() {
    if (this.sentenceCoreMap.get(CoreAnnotations.MentionsAnnotation.class) != null) {
      entityMentions = this.sentenceCoreMap.get(CoreAnnotations.MentionsAnnotation.class).
          stream().map(coreMapEntityMention -> new CoreEntityMention(this,coreMapEntityMention)).collect(Collectors.toList());
    }
  }

  /** get the document this sentence is in **/
  public CoreDocument document() {
    return document;
  }

  /** get the underlying CoreMap if need be **/
  public CoreMap coreMap() {
    return sentenceCoreMap;
  }

  /** full text of the sentence **/
  public String text() {
    return sentenceCoreMap.get(CoreAnnotations.TextAnnotation.class);
  }

  /** char offsets of mention **/
  public Pair<Integer,Integer> charOffsets() {
    int beginCharOffset = this.sentenceCoreMap.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
    int endCharOffset = this.sentenceCoreMap.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
    return new Pair<>(beginCharOffset,endCharOffset);
  }

  /** list of tokens **/
  public List<CoreLabel> tokens() {
    return sentenceCoreMap.get(CoreAnnotations.TokensAnnotation.class);
  }

  /** list of tokens as String **/
  public List<String> tokensAsStrings() {
    return tokens().stream().map(CoreLabel::word).collect(Collectors.toList()); }

  /** list of pos tags **/
  public List<String> posTags() { return tokens().stream().map(CoreLabel::tag).collect(Collectors.toList()); }

  /** list of lemma tags **/
  public List<String> lemmas() { return tokens().stream().map(CoreLabel::lemma).collect(Collectors.toList()); }

  /** list of ner tags **/
  public List<String> nerTags() { return tokens().stream().map(CoreLabel::ner).collect(Collectors.toList()); }

  /** constituency parse **/
  public Tree constituencyParse() {
    return sentenceCoreMap.get(TreeCoreAnnotations.TreeAnnotation.class);
  }

  /** Tregex - find subtrees of interest with a general Tregex pattern **/
  public List<Tree> tregexResultTrees(String s) {
    // the patterns are cached by computeIfAbsent, so we don't wastefully recompile a TregexPattern every sentence
    return tregexResultTrees(patternCache.computeIfAbsent(s, compilePattern));
  }

  public List<Tree> tregexResultTrees(TregexPattern p) {
    // throw a RuntimeException if no constituency parse available to signal to user to use "parse" annotator
    if (constituencyParse() == null)
      throw new RuntimeException("Error: Attempted to run Tregex on sentence without a constituency parse.  " +
              "To use this method you must annotate the document with a constituency parse using the 'parse' " +
              "annotator.");
    List<Tree> results = new ArrayList<>();
    TregexMatcher matcher = p.matcher(constituencyParse());
    while (matcher.find()) {
      results.add(matcher.getMatch());
    }
    return results;
  }

  public List<String> tregexResults(TregexPattern p) {
    return tregexResultTrees(p).stream().map(treeToSpanString).collect(Collectors.toList());
  }

  public List<String> tregexResults(String s) {
    return tregexResultTrees(s).stream().map(treeToSpanString).collect(Collectors.toList());
  }

  /** return noun phrases, assuming NP is the label **/
  public List<Tree> nounPhraseTrees() {
    return tregexResultTrees(nounPhrasePattern);
  }

  public List<String> nounPhrases() {
    return nounPhraseTrees().stream().map(treeToSpanString).collect(Collectors.toList());
  }

  /** return verb phrases, assuming VP is the label **/
  public List<Tree> verbPhraseTrees() {
    return tregexResultTrees(verbPhrasePattern);
  }

  public List<String> verbPhrases() {
    return verbPhraseTrees().stream().map(treeToSpanString).collect(Collectors.toList());
  }

  /** dependency parse **/
  public SemanticGraph dependencyParse() {
    return sentenceCoreMap.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);
  }

  /** sentiment **/
  public String sentiment() {
    return sentenceCoreMap.get(SentimentCoreAnnotations.SentimentClass.class);
  }

  /** sentiment tree **/
  public Tree sentimentTree() {
    return sentenceCoreMap.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
  }

  /** list of entity mentions **/
  public List<CoreEntityMention> entityMentions() { return this.entityMentions; }

  /** list of KBP relations found **/
  public List<RelationTriple> relations() {
    return sentenceCoreMap.get(CoreAnnotations.KBPTriplesAnnotation.class);
  }

  @Override
  public String toString() {
    return coreMap().toString();
  }
}
