# deploy file 

## Placeholders
The placeholders are to be replaced with actaul values during the image build and container starts. The placeholders are of the format *<placeholder name>*. The placeholder must be completely replaced with real value including angle brackets, for example a placeholder to be replaced with 'myvalue' should be like this.

```
some command or code <placeholder> more code here
# replaced
some command or code myvalue more code here
```

## Place holders and meanings
1. **\<dumpfile path\>**: Path to .sql dumpfile which needs to be used with a specific site.
1. **\<database name\>**: Name of database where dumpfile is imported. 
1. **\<database username\>**: Name of MySQL user with privilages over the said database. 
1. **\<database user password\>**: Password for the user mentioned above. 
1. **\<image name\>**: Name of image produced after podman-compose build. 
1. **\<container name\>**: Container started using the image.
1. **\<github cloned repo\>**: Cloned github repository to be containerized. 


 
