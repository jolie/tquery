define lookup
{
  with( lookupRequest ){
    .leftData << bios;
    .leftPath = "awards";
    .rightPath = "awards";
    .rightData << bios;
    .dstPath = "dstPath"
  }
}

define lookup_compound
{
  with( lookupRequest ){
    .leftData << bios;
    .leftPath = "awards.award";
    .rightPath = "awards.award";
    .rightData << bios;
    .dstPath = "dstPath"
  }
}