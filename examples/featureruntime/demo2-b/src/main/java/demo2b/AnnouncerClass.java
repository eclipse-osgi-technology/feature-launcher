package demo2b;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(configurationPid = "demo.config.b", property = "name=Tim")
public class AnnouncerClass {

	@Activate
	void start(Map<String, Object> map) {
		System.out.println("Greetings " + map.get("name") + " from B");
	}
}
