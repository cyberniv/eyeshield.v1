# Eye Shield - Test API Responses

## Example API Calls and Responses

### **Test 1: Safe Website**

#### Request
```bash
curl -X POST http://localhost:8080/apis/urls/analyse-url \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.google.com"}'
```

#### Response
```json
{
  "url": "https://www.google.com",
  "vt_raw_stats": {
    "malicious": 0,
    "undetected": 5,
    "harmless": 78,
    "suspicious": 0
  },
  "bedrock_analysis": "{\"verdict\": \"Safe\", \"confidence_score\": 98, \"summary\": \"Google is a well-established search engine with excellent security reputation. No threats detected across VirusTotal scanners. Safe to visit.\"}"
}
```

**Expected App Display**:
- Verdict Badge: "SAFE" (Green)
- Confidence: 98%
- VirusTotal: 0 Malicious, 78 Harmless, 5 Undetected

---

### **Test 2: Suspicious Website**

#### Request
```bash
curl -X POST http://localhost:8080/apis/urls/analyse-url \
  -H "Content-Type: application/json" \
  -d '{"url": "https://suspicious-domain.xyz"}'
```

#### Response
```json
{
  "url": "https://suspicious-domain.xyz",
  "vt_raw_stats": {
    "malicious": 3,
    "undetected": 12,
    "harmless": 28,
    "suspicious": 15
  },
  "bedrock_analysis": "{\"verdict\": \"Warning\", \"confidence_score\": 65, \"summary\": \"This domain shows mixed threat indicators. Multiple security vendors flagged it as suspicious. The domain's reputation is poor, and it displays phishing-like characteristics. Proceed with caution.\"}"
}
```

**Expected App Display**:
- Verdict Badge: "WARNING" (Amber)
- Confidence: 65%
- VirusTotal: 3 Malicious, 28 Harmless, 12 Undetected, 15 Suspicious

---

### **Test 3: Dangerous Website (Phishing)**

#### Request
```bash
curl -X POST http://localhost:8080/apis/urls/analyse-url \
  -H "Content-Type: application/json" \
  -d '{"url": "https://bank-secure-login-verify.com"}'
```

#### Response
```json
{
  "url": "https://bank-secure-login-verify.com",
  "vt_raw_stats": {
    "malicious": 45,
    "undetected": 3,
    "harmless": 8,
    "suspicious": 10
  },
  "bedrock_analysis": "{\"verdict\": \"Dangerous\", \"confidence_score\": 94, \"summary\": \"This URL is a known phishing site impersonating a banking institution. VirusTotal flagged it as malicious (45 vendors). Contains credential harvesting forms and SSL certificate mimicry. DO NOT VISIT - Risk of identity theft.\"}"
}
```

**Expected App Display**:
- Verdict Badge: "DANGEROUS" (Red)
- Confidence: 94%
- VirusTotal: 45 Malicious, 8 Harmless, 3 Undetected, 10 Suspicious

---

## Backend Implementation Example (Node.js + Express)

```javascript
const express = require('express');
const app = express();
app.use(express.json());

app.post('/apis/urls/analyse-url', (req, res) => {
  const { url } = req.body;
  
  // Simulate API call to VirusTotal and Bedrock
  const analysis = {
    url: url,
    vt_raw_stats: {
      malicious: Math.floor(Math.random() * 50),
      undetected: Math.floor(Math.random() * 20),
      harmless: Math.floor(Math.random() * 80),
      suspicious: Math.floor(Math.random() * 20)
    },
    bedrock_analysis: JSON.stringify({
      verdict: ["Safe", "Warning", "Dangerous"][Math.floor(Math.random() * 3)],
      confidence_score: Math.floor(Math.random() * 40) + 60,
      summary: "This URL has been analyzed by our security AI engine. Results shown above."
    })
  };
  
  res.json(analysis);
});

app.listen(8080, () => console.log('Server running on port 8080'));
```

---

## Android Test Instructions

### **Step 1: Start Backend Server**
```bash
# In separate terminal, start your API server
node server.js  # or your backend implementation
```

### **Step 2: Update Base URL** (if needed)
Edit `MainActivity.kt`:
```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/"  // For emulator
// OR
private const val BASE_URL = "http://192.168.x.x:8080/"  // For device (your PC IP)
```

### **Step 3: Run App**
```bash
./gradlew installDebug
```

### **Step 4: Test URLs**
Try these in the app:
- ✅ `https://www.google.com` (should be Safe)
- ✅ `https://www.github.com` (should be Safe)
- ⚠️ `https://suspicious-domain.xyz` (should be Warning)
- ❌ `https://phishing-site.com` (should be Dangerous)

---

## JSON Response Format Rules

The API MUST return:
1. **url** (string): The analyzed URL
2. **vt_raw_stats** (object):
   - malicious (int)
   - undetected (int)
   - harmless (int)
   - suspicious (int)
3. **bedrock_analysis** (string): JSON string containing:
   - verdict (string): "Safe", "Warning", or "Dangerous"
   - confidence_score (int): 0-100
   - summary (string): Human-readable explanation

### Optional: Bedrock Analysis with Markdown
If your backend wraps JSON in markdown code fences:
```json
{
  "bedrock_analysis": "```json\n{\"verdict\": \"Safe\", \"confidence_score\": 98, \"summary\": \"...\"}\n```"
}
```
The app will automatically strip the markdown and parse the JSON.

---

## Testing Checklist

- [ ] Test with valid HTTP URL
- [ ] Test with valid HTTPS URL
- [ ] Test with domain-only URL (no protocol)
- [ ] Test with invalid URL (should show error)
- [ ] Test with empty input (should show error)
- [ ] Test network timeout (should show error)
- [ ] Test "Continue to Site" button
- [ ] Test "Go Back" button
- [ ] Test WebView back navigation
- [ ] Test with various verdict scores

---

## Common Issues & Solutions

### **Issue: "Scan failed: Failed to connect"**
**Solution**: Ensure backend is running on correct port
```bash
# Check if port 8080 is listening
netstat -tlnp | grep :8080

# Or use curl to test
curl -X POST http://localhost:8080/apis/urls/analyse-url \
  -H "Content-Type: application/json" \
  -d '{"url": "https://test.com"}'
```

### **Issue: "Scan failed: Invalid JSON"**
**Solution**: Ensure bedrock_analysis is valid JSON string
- Remove any markdown code fences
- Escape quotes properly
- Use JSON encoder for special characters

### **Issue: App crashes on Analysis Screen**
**Solution**: Check that BedrockAnalysis fields match response:
- verdict (required, string)
- confidence_score (required, int, with @SerializedName)
- summary (required, string)

### **Issue: WebView shows blank page**
**Solution**: 
- Verify URL format is correct
- Check network connectivity
- Ensure URL is properly decoded from navigation parameter
- Check if JavaScript is enabled

---

## Performance Tips

1. **Timeout**: Set to 30 seconds for slow networks
2. **Retry Logic**: Implement exponential backoff
3. **Caching**: Consider caching scan results per session
4. **Images**: Use webp format for  UI assets
5. **Memory**: Monitor WebView for memory leaks

---

## Security Considerations

1. ✅ Use HTTPS for all API calls in production
2. ✅ Validate all user inputs
3. ✅ Never store sensitive URLs in logs
4. ✅ Implement API rate limiting
5. ✅ Use certificate pinning for critical endpoints
6. ✅ Obfuscate API keys if needed

---

**Last Updated**: April 5, 2026  
**Status**: Ready for Integration Testing
