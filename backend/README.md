# HomeWork 后端

后端是一个 Java 21 / Spring Boot / Maven 多模块工程：

```text
backend/
├── common/
├── model/
└── web/
    ├── web-app/
    └── web-admin/
```

在仓库根目录运行：

```bash
mvn -f backend/pom.xml test
mvn -f backend/pom.xml verify
```
