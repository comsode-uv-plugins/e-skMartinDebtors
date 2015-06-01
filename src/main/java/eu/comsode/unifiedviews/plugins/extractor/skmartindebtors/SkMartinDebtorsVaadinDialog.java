package eu.comsode.unifiedviews.plugins.extractor.skmartindebtors;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractDialog;

/**
 * Vaadin configuration dialog .
 */
public class SkMartinDebtorsVaadinDialog extends AbstractDialog<SkMartinDebtorsConfig_V1> {

    public SkMartinDebtorsVaadinDialog() {
        super(SkMartinDebtors.class);
    }

    @Override
    public void setConfiguration(SkMartinDebtorsConfig_V1 c) throws DPUConfigException {

    }

    @Override
    public SkMartinDebtorsConfig_V1 getConfiguration() throws DPUConfigException {
        final SkMartinDebtorsConfig_V1 c = new SkMartinDebtorsConfig_V1();

        return c;
    }

    @Override
    public void buildDialogLayout() {
    }

}
