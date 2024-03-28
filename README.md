# Datei-Upload-Service

Dieses Projekt bietet einen Datei-Upload-Service mit der Generierung von Bildvorschauen unter Verwendung von Spring WebFlux und Reactor's Mono. Der Service ermöglicht es Benutzern, verschiedene Arten von Dateien, einschließlich Bildern und Dokumenten, hochzuladen, und ruft Dateien anhand ihrer eindeutigen Kennungen ab. Darüber hinaus generiert der Service Vorschauen für hochgeladene Bilder.


### Voraussetzungen

- **Java 11 oder höher**
- **Maven**

### Datenbankkonfiguration

Bearbeiten Sie die Datei application.properties, um die Datenbankeinstellungen zu konfigurieren. Datenbank skript in `resources/sql_script`

### API-Endpunkte

- **Datei hochladen:**
    - **Endpunkt:** `POST /api/upload`
    - **Anfrage:**
        - Verwenden Sie ein Tool wie `Postman`, um eine Datei hochzuladen.
        - Fügen Sie die Datei als Teil von `multipart/form-data` hinzu.
        - Geben Sie den Parameter `messageid` als Abfrageparameter an.
    - **Antwort:**
        - Gibt ein `FileDTO` zurück, das Informationen über die hochgeladene Datei enthält.

- **Datei herunterladen:**
    - **Endpunkt:** `GET /api/download/{id}`
    - **Anfrage:**
        - Ersetzen Sie `{id}` durch die eindeutige Kennung der Datei, die Sie herunterladen möchten.
    - **Antwort:**
        - Lädt die Datei mit der angegebenen ID herunter.

- **Datei nach ID abrufen:**
    - **Endpunkt:** `GET /api/files/{id}`
    - **Anfrage:**
        - Ersetzen Sie `{id}` durch die eindeutige Kennung der Datei, die Sie abrufen möchten.
    - **Antwort:**
        - Gibt ein `FileDTO` zurück, das Informationen über die angeforderte Datei enthält.

## Zusätzliche Hinweise

- **Erlaubte Dateitypen:**
    - Der Service beschränkt den Datei-Upload auf bestimmte MIME-Typen, einschließlich Bilder und Dokumente. Überprüfen Sie die `allowedFileTypes`-Menge in der Klasse `FileUploadServiceImpl` für die vollständige Liste.

- **Bildvorschau:**
    - Wenn die hochgeladene Datei ein Bild ist, generiert der Service eine Vorschau mit verminderten Abmessungen. Die Vorschau ist mit der Originaldatei verknüpft und kann über den Endpunkt `GET /api/files/{id}` abgerufen werden.

- **Reaktive Programmierung mit Mono:**
    - Das Projekt nutzt Spring WebFlux und Reactor's `Mono`, um asynchrone, nicht blockierende Operationen zu behandeln. Die reaktive Programmierung ermöglicht eine effiziente Bearbeitung von gleichzeitigen Anfragen und verbessert die Gesamtreaktionsfähigkeit des Dienstes.
