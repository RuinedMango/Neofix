package org.to2mbn.jmccc.mcdownloader.provider;

import java.io.IOException;

import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.mcdownloader.download.tasks.ResultProcessor;
import org.to2mbn.jmccc.util.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class Xml2JsonDecoder implements ResultProcessor<byte[], JSONObject> {
	public JSONObject process(byte[] data) {
		String dataString = IOUtils.toString(data);
    	System.out.print(dataString);
        // Create an XmlMapper to read XML data
        XmlMapper xmlMapper = new XmlMapper();
        // Read the XML file and convert it to a JsonNode
        JsonNode jsonNode = null;
		try {
			jsonNode = xmlMapper.readTree(dataString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // Create an ObjectMapper for converting JsonNode to String JSON
        ObjectMapper jsonMapper = new ObjectMapper();
        // Convert the JsonNode to JSON String
        String jsonString = null;
		try {
			jsonString = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return new JSONObject(jsonString);
    }
}
