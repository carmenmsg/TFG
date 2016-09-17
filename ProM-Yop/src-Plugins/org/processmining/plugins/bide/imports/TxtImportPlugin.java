package org.processmining.plugins.bide.imports;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.processmining.bide.input.sequence_database_list_integers.SequenceDatabase2;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

 
/**
 * @author Carmen
 */
@Plugin(name = "Import TXT",
        parameterLabels = { "Filename" },
        returnLabels = { "TXT" },
        returnTypes = {SequenceDatabase2.class })

@UIImportPlugin(description = "Extract Instances(txt)",
                extensions = { "txt" })

public class TxtImportPlugin extends AbstractImportPlugin {
  
  @Override
  protected SequenceDatabase2 importFromStream(final PluginContext context,
                                    final InputStream input,
                                    final String filename,
                                    final long fileSizeInBytes) throws IOException {
    try {
      context.getFutureResult(0).setLabel("txt Instances from " + filename);
      File appDir = new File(System.getProperty("user.dir"));
      URI uri = new URI(appDir.toURI() + "resources/" + filename);
      //System.out.println("Voy por aquí: Import resources");
      //System.out.println(uri.getPath().toString());
      //DataSource source = new DataSource(uri.getPath().toString());
      //Instances instances = source.getDataSet();
      //return instances;
      SequenceDatabase2 seqDatabase = new SequenceDatabase2();
      seqDatabase.loadFile(uri.getPath().toString());
      //System.out.println("Voy por aquí: Import return");
      return seqDatabase;
    } catch (Exception e) {
        System.out.println(e.getMessage());
        return null;
    }
  }
}
