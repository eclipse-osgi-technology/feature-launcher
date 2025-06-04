package demo2a;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(configurationPid = "demo.config.a", property = "name=World")
public class AnnouncerClass {

	@Activate
	void start(Map<String, Object> map) {
		System.out.println("Hello " + map.get("name") + " from A");
	}
}
