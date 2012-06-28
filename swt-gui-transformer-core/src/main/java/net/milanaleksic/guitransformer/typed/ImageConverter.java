package net.milanaleksic.guitransformer.typed;

import net.milanaleksic.guitransformer.TransformerException;
import net.milanaleksic.guitransformer.providers.ImageProvider;
import org.codehaus.jackson.JsonNode;
import org.eclipse.swt.graphics.Image;

import javax.inject.Inject;

/**
 * User: Milan Aleksic
 * Date: 4/21/12
 * Time: 7:30 PM
 */
public class ImageConverter extends TypedConverter<Image> {

    @Inject
    private ImageProvider imageProvider;

    @Override
    public Image getValueFromJson(JsonNode node) throws TransformerException {
        return imageProvider.provideImageForName(node.asText());
    }

}