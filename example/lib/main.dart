import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:request_permission/request_permission.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  //Tries to only allow Portrait mode, if an Error occures
  //it launches anyway but with Portrait and landscape
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]).whenComplete(() {
    runApp(new MyApp());
  });
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String results;

  String permission;
  List<String> permissions;

  RequestPermission requestPermission;

  @override
  void initState() {
    super.initState();
    results = "Empty";

    permission = "android.permission.CAMERA";
    permissions = [
      permission,
      "android.permission.CALL_PHONE",
    ];

    requestPermission = RequestPermission.instace;

    requestPermission.results.listen((event) {
      setState(() {
        results = """
        permissions: ${event.permissions}
        requestCode: ${event.requestCode}
        grantResults: ${event.grantResults}
        """;
      });
      print("""
      
      $results
            
      grantedPermissions: ${event.grantedPermissions}

      -----------------------------------
      
      """);
    });

    requestPermission.setLogLevel(LogLevel.none);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: SafeArea(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              RaisedButton(
                child: Text("request single"),
                onPressed: () {
                  requestPermission.requestAndroidPermission(100, permission);
                },
              ),
              RaisedButton(
                child: Text("request multiple"),
                onPressed: () {
                  requestPermission.requestMultipleAndroidPermissions(
                    101,
                    permissions,
                  );
                },
              ),
              RaisedButton(
                child: Text("has"),
                onPressed: () async {
                  bool has =
                      await requestPermission.hasAndroidPermission(permission);
                  print("""
                  permission: $permission
                  hasPermission: $has
                  """);
                },
              ),
              Container(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  color: Colors.amber[200],
                ),
                padding: EdgeInsets.all(6),
                child: Text(
                  results,
                  style: TextStyle(fontWeight: FontWeight.w600),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
