package org.jetbrains.jps.intellilang.model.impl;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.JDOMExternalizerUtil;
import org.jdom.Element;

import org.jetbrains.jps.intellilang.instrumentation.InstrumentationType;
import org.jetbrains.jps.intellilang.model.JpsIntelliLangExtensionService;
import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.model.serialization.JpsGlobalExtensionSerializer;

/**
 * @author Eugene Zhuravlev
 *         Date: 11/29/12
 */
public class JpsIntelliLangConfigurationSerializer extends JpsGlobalExtensionSerializer {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.jps.intellilang.model.impl.JpsIntelliLangConfigurationSerializer");
  private static final String INSTRUMENTATION_TYPE_NAME = "INSTRUMENTATION";
  private static final String PATTERN_ANNOTATION_NAME = "PATTERN_ANNOTATION";

  public JpsIntelliLangConfigurationSerializer() {
    super("IntelliLang.xml", "LanguageInjectionConfiguration");
  }

  @Override
  public void loadExtension( JpsGlobal global,  Element componentTag) {
    final JpsIntelliLangConfigurationImpl configuration = new JpsIntelliLangConfigurationImpl();

    final String annotationName = JDOMExternalizerUtil.readField(componentTag, PATTERN_ANNOTATION_NAME);
    if (annotationName != null) {
      configuration.setPatternAnnotationClassName(annotationName);
    }

    final String instrumentationType = JDOMExternalizerUtil.readField(componentTag, INSTRUMENTATION_TYPE_NAME);
    if (instrumentationType != null) {
      try {
        final InstrumentationType type = InstrumentationType.valueOf(instrumentationType);
        configuration.setInstrumentationType(type);
      }
      catch (IllegalArgumentException ignored) {
        LOG.info(ignored);
      }
    }

    JpsIntelliLangExtensionService.getInstance().setConfiguration(global, configuration);
  }

  @Override
  public void loadExtensionWithDefaultSettings( JpsGlobal global) {
    JpsIntelliLangExtensionService.getInstance().setConfiguration(global, new JpsIntelliLangConfigurationImpl());
  }

  @Override
  public void saveExtension( JpsGlobal global,  Element componentTag) {
  }
}
