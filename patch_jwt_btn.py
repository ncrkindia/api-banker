import re

# 1. Update MainFrame.java
with open('src/main/java/in/slpro/japi/ui/MainFrame.java', 'r', encoding='utf-8') as f:
    mf = f.read()

new_method = """    public void openJwtDecoder(String initialToken) {
        RequestModel req = new RequestModel();
        req.setName("JWT Decoder");
        req.setType("jwt");
        if (initialToken != null) req.setBodyRawContent(initialToken);
        JwtDecoderPanel panel = new JwtDecoderPanel(this, req);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("JWT Decoder", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("JWT Decoder", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openJwtDecoder() {
        openJwtDecoder(null);
    }"""

mf = re.sub(r'    public void openJwtDecoder\(\) \{[\s\S]*?workspaceTabs\.setSelectedIndex\(idx\);\s*\}', new_method, mf)

with open('src/main/java/in/slpro/japi/ui/MainFrame.java', 'w', encoding='utf-8') as f:
    f.write(mf)

# 2. Update RequestPanel.java
with open('src/main/java/in/slpro/japi/ui/RequestPanel.java', 'r', encoding='utf-8') as f:
    rp = f.read()

rp = re.sub(
    r'(atkPanel\.add\(oauth2AccessTokenField, BorderLayout\.CENTER\);)',
    r'\1\n        JButton oauth2DecodeJwtBtn = new JButton("Decode JWT");\n        oauth2DecodeJwtBtn.addActionListener(e -> {\n            String token = oauth2AccessTokenField.getText();\n            if (token != null && !token.isBlank()) {\n                mainFrame.openJwtDecoder(token);\n            }\n        });\n        atkPanel.add(oauth2DecodeJwtBtn, BorderLayout.WEST);',
    rp
)
with open('src/main/java/in/slpro/japi/ui/RequestPanel.java', 'w', encoding='utf-8') as f:
    f.write(rp)

# 3. Update CollectionPanel.java
with open('src/main/java/in/slpro/japi/ui/CollectionPanel.java', 'r', encoding='utf-8') as f:
    cp = f.read()

cp = re.sub(
    r'(atkPanel\.add\(oauth2AccessTokenField, BorderLayout\.CENTER\);)',
    r'\1\n        JButton oauth2DecodeJwtBtn = new JButton("Decode JWT");\n        oauth2DecodeJwtBtn.addActionListener(e -> {\n            String token = oauth2AccessTokenField.getText();\n            if (token != null && !token.isBlank()) {\n                mainFrame.openJwtDecoder(token);\n            }\n        });\n        atkPanel.add(oauth2DecodeJwtBtn, BorderLayout.WEST);',
    cp
)
with open('src/main/java/in/slpro/japi/ui/CollectionPanel.java', 'w', encoding='utf-8') as f:
    f.write(cp)

print("Patch applied successfully.")
