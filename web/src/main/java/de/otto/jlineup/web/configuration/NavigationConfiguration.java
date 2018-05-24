package de.otto.jlineup.web.configuration;

import de.otto.edison.configuration.EdisonApplicationProperties;
import de.otto.edison.navigation.NavBar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import static de.otto.edison.navigation.NavBarItem.navBarItem;

@Component
@EnableConfigurationProperties(EdisonApplicationProperties.class)
public class NavigationConfiguration {

    @Autowired
    public NavigationConfiguration(final NavBar mainNavBar,
                                   final EdisonApplicationProperties  properties) {
        mainNavBar.register(navBarItem(0, "Status", String.format("%s/status", properties.getManagement().getBasePath())));
        mainNavBar.register(navBarItem(1, "Reports", String.format("%s/reports", properties.getManagement().getBasePath())));
    }
}

